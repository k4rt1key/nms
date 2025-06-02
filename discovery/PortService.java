package org.nms.discovery;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClientOptions;
import org.nms.App;
import org.nms.constants.Database;
import org.nms.constants.Eventbus;
import org.nms.constants.Global;
import org.nms.utils.DbUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import static org.nms.App.LOGGER;

public class PortService extends AbstractVerticle
{
    @Override
    public void start()
    {
        vertx.eventBus().localConsumer(Eventbus.PORT_CHECK_START, this::portCheck);
    }

    @Override
    public void stop()
    {
        LOGGER.info("PortService Verticle Stopped");
    }

    public void portCheck(Message<JsonObject> message)
    {
        var discoveryId = message.body().getInteger(Database.DiscoveryResult.DISCOVERY_PROFILE);

        var ips = message.body().getJsonArray(Database.Discovery.IP);

        var port = message.body().getInteger(Database.Discovery.PORT);

        var results = new JsonArray();

        if (ips == null || ips.isEmpty() || port < 1 || port > 65535) return;

        LOGGER.debug("received port check request for port: " + port + " ips: " + ips.encode());

        var portCheck = new ArrayList<Future<JsonObject>>();

        var successfulIps = new JsonArray();

        var failedResults = new JsonArray();

        for (var ip : ips)
        {
            var checkPortPromise = Promise.<JsonObject>promise();

            portCheck.add(checkPortPromise.future());

            var failedResult = new JsonObject()
                    .put(Database.Discovery.IP, ip.toString())
                    .put(Database.Discovery.PORT, port);

            try
            {
                App.VERTX.createNetClient(new NetClientOptions().setConnectTimeout(Global.PORT_CHECK_TIMEOUT * 1000))
                        .connect(port, ip.toString(), asyncResult ->
                        {
                            try
                            {
                                if (asyncResult.succeeded())
                                {
                                    var socket = asyncResult.result();

                                    successfulIps.add(ip);

                                    checkPortPromise.complete();

                                    socket.close();
                                }
                                else
                                {
                                    var cause = asyncResult.cause();

                                    var errorMessage = cause != null ? cause.getMessage() : "Unknown error";

                                    failedResult
                                            .put(Database.DiscoveryResult.DISCOVERY_PROFILE, discoveryId)
                                            .put(Database.DiscoveryResult.CREDENTIAL_PROFILE, null)
                                            .put(Database.DiscoveryResult.STATUS, Database.DiscoveryResult.FAILED)
                                            .put(Database.DiscoveryResult.MESSAGE,
                                                    errorMessage.contains("Connection refused")
                                                            ? "Port " + port + " is closed on " + ip
                                                            : errorMessage)
                                                    .put(Database.DiscoveryResult.TIME, ZonedDateTime.now(ZoneId.of(Global.INDIA_ZONE_NAME)).toString());

                                    failedResults.add(failedResult);

                                    checkPortPromise.complete();
                                }
                            }
                            catch (Exception exception)
                            {
                                failedResult
                                        .put(Database.DiscoveryResult.DISCOVERY_PROFILE, discoveryId)
                                        .put(Database.DiscoveryResult.CREDENTIAL_PROFILE, null)
                                        .put(Database.DiscoveryResult.STATUS, Database.DiscoveryResult.FAILED)
                                        .put(Database.DiscoveryResult.MESSAGE, "Something went wrong during port scan")
                                        .put(Database.DiscoveryResult.TIME, ZonedDateTime.now(ZoneId.of(Global.INDIA_ZONE_NAME)).toString());

                                failedResults.add(failedResult);

                                LOGGER.error("Something went wrong, error: " + exception.getMessage());

                                checkPortPromise.complete();
                            }

                        });
            }
            catch (Exception exception)
            {
                failedResult
                        .put(Database.DiscoveryResult.DISCOVERY_PROFILE, discoveryId)
                        .put(Database.DiscoveryResult.CREDENTIAL_PROFILE, null)
                        .put(Database.DiscoveryResult.STATUS, Database.DiscoveryResult.FAILED)
                        .put(Database.DiscoveryResult.MESSAGE, "Something went wrong scanning port")
                        .put(Database.DiscoveryResult.TIME, ZonedDateTime.now(ZoneId.of(Global.INDIA_ZONE_NAME)).toString());

                failedResults.add(failedResult);

                checkPortPromise.complete();
            }
        }

        Future.all(portCheck)
                .onComplete(asyncResult ->
                {
                    if (asyncResult.succeeded())
                    {
                        LOGGER.debug("Port check passed ips: " + results.encode());

                        vertx.eventBus().send(Eventbus.CREDENTIAL_CHECK_START, new JsonObject()
                                .put(Database.DiscoveryResult.DISCOVERY_PROFILE, discoveryId)
                                .put(Database.DiscoveryResult.IP, successfulIps)
                                .put(Database.Discovery.PORT, port)
                        );

                        LOGGER.debug("Port check failed ips: " + results.encode());

                        var queryRequest = DbUtils.buildRequest(
                            Database.Table.DISCOVERY_RESULT,
                            Database.Operation.BATCH_INSERT,
                            null,
                            null,
                            failedResults
                        );

                        if(!failedResults.isEmpty()) vertx.eventBus().send(Eventbus.EXECUTE_QUERY, queryRequest);
                    }
                    else
                    {
                        LOGGER.error("Error in port check: " + asyncResult.cause().getMessage());
                    }
                });
    }
}
