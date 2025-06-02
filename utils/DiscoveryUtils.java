package org.nms.utils;

import inet.ipaddr.IPAddressString;
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
import org.nms.validators.Validators;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.nms.App.LOGGER;
import static org.nms.App.VERTX;

public class DiscoveryUtils
{
    public static JsonArray getIpsFromString(String ip, String ipType)
    {
        var ips = new JsonArray();

        if (Validators.validateIpWithIpType(ip, ipType)) return ips;

        try
        {
            if ("RANGE".equalsIgnoreCase(ipType))
            {
                var parts = ip.split("-");

                var startIp = new IPAddressString(parts[0].trim()).getAddress();

                var endIp = new IPAddressString(parts[1].trim()).getAddress();

                var address = startIp.toSequentialRange(endIp);

                address.getIterable().forEach(ipAddress -> ips.add(ipAddress.toString()));
            }
            else if ("CIDR".equalsIgnoreCase(ipType))
            {
                new IPAddressString(ip)
                        .getSequentialRange()
                        .getIterable()
                        .forEach(ipAddress -> ips.add(ipAddress.toString()));
            }
            else if ("SINGLE".equalsIgnoreCase(ipType))
            {
                var singleAddr = new IPAddressString(ip).getAddress();

                ips.add(singleAddr.toString());
            }

            return ips;
        }
        catch (Exception exception)
        {
            LOGGER.error("Failed to convert ip string to JsonArray of ip, error: " + exception.getMessage() );

            return new JsonArray();
        }
    }

    public static void pingIps(Message<JsonObject> message)
    {
        var discovery = message.body();

        var ip = discovery.getString(Database.Discovery.IP);

        var ipType = discovery.getString(Database.Discovery.IP_TYPE);

        var ips = DiscoveryUtils.getIpsFromString(ip, ipType);

        // Validation
        if (ips == null || ips.isEmpty()) return;

        LOGGER.debug("Received ping check request for ips: " + ips.encode());

        // Prepare fping command
        var command = new String[ips.size() + 3];
        command[0] = "fping";
        command[1] = "-c1";
        command[2] = "-q";

        for (var i = 0; i < ips.size(); i++)
        {
            command[i + 3] = ips.getString(i);
        }

        // Execute fping command
        VERTX.executeBlocking((Promise<Void> promise) ->
        {
            Process process = null;

            BufferedReader reader = null;

            try
            {
                var processBuilder = new ProcessBuilder(command);

                processBuilder.redirectErrorStream(true);

                process = processBuilder.start();

                // Process timeout
                var completed = process.waitFor(Global.BASE_PLUGIN_TIMEOUT + ((long) Global.DISCOVERY_TIMEOUT_PER_IP * ips.size()), TimeUnit.SECONDS);

                if (!completed)
                {
                    process.destroyForcibly();

                    var failedResult = new JsonArray();

                    for (var i = 0; i < ips.size(); i++)
                    {
                        failedResult.add(new JsonObject()
                                .put(Database.DiscoveryResult.DISCOVERY_PROFILE, discovery.getInteger(Database.Discovery.ID))
                                .put(Database.DiscoveryResult.CREDENTIAL_PROFILE, null)
                                .put(Database.DiscoveryResult.IP, String.valueOf(ips.getValue(i)))
                                .put(Database.DiscoveryResult.STATUS, Database.DiscoveryResult.FAILED)
                                .put(Database.DiscoveryResult.MESSAGE, "Ping process timed out")
                                .put(Database.DiscoveryResult.TIME, ZonedDateTime.now(ZoneId.of(Global.INDIA_ZONE_NAME)).toString())
                        );
                    }

                    var queryRequest = DbUtils.buildRequest(
                            Database.Table.DISCOVERY_RESULT,
                            Database.Operation.INSERT,
                            null,
                            null,
                            failedResult
                    );

                    VERTX.eventBus().send(Eventbus.EXECUTE_QUERY, queryRequest);

                    return;
                }

                // read results
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;

                var successfulIps = new JsonArray();

                var failedResult = new JsonArray();

                while ((line = reader.readLine()) != null)
                {
                    var parts = line.split(":");

                    if (parts.length < 2)
                    {
                        continue;
                    }

                    var resultIp = parts[0].trim();

                    var stats = parts[1].trim();

                    var isSuccess = !stats.contains("100%");

                    if(isSuccess)
                    {
                        successfulIps.add(resultIp);
                    }
                    else
                    {
                        var result = new JsonObject()
                                .put(Database.DiscoveryResult.DISCOVERY_PROFILE, discovery.getInteger(Database.Discovery.ID))
                                .put(Database.DiscoveryResult.CREDENTIAL_PROFILE, null)
                                .put(Database.DiscoveryResult.IP, resultIp)
                                .put(Database.DiscoveryResult.STATUS, Database.DiscoveryResult.FAILED)
                                .put(Database.DiscoveryResult.TIME, ZonedDateTime.now(ZoneId.of(Global.INDIA_ZONE_NAME)).toString())
                                .put(Database.DiscoveryResult.MESSAGE, "Ping check failed: 100% packet loss");

                        failedResult.add(result);
                    }
                }

                var queryRequest = DbUtils.buildRequest(
                        Database.Table.DISCOVERY_RESULT,
                        Database.Operation.BATCH_INSERT,
                        null,
                        null,
                        failedResult
                );

                LOGGER.debug("ping check failed ips: " + failedResult.encode());

                if(!failedResult.isEmpty()) VERTX.eventBus().send(Eventbus.EXECUTE_QUERY, queryRequest);

                LOGGER.debug("ping check passed ips: " + successfulIps.encode());

                VERTX.eventBus().send(Eventbus.PORT_CHECK_START, new JsonObject()
                        .put(Database.Discovery.IP, successfulIps)
                        .put(Database.Discovery.PORT, discovery.getInteger(Database.Discovery.PORT))
                        .put(Database.DiscoveryResult.DISCOVERY_PROFILE, discovery.getInteger(Database.Discovery.ID))
                );

            }
            catch (Exception exception)
            {
                LOGGER.error("Error during ping check: " + exception.getMessage());
            }
            finally
            {
                if (reader != null)
                {
                    try
                    {
                        reader.close();
                    }
                    catch (Exception exception)
                    {
                        LOGGER.error("Error closing resource: " + exception.getMessage());
                    }
                }

                if (process != null && process.isAlive())
                {
                    try
                    {
                        process.destroyForcibly();
                    }
                    catch (Exception exception)
                    {
                        LOGGER.error("Error destroying ping process: " + exception.getMessage());
                    }
                }
            }

        }, false);
    }

    public static void portCheck(Message<JsonObject> message)
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

                        VERTX.eventBus().send(Eventbus.CREDENTIAL_CHECK_START, new JsonObject()
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

                        if(!failedResults.isEmpty()) VERTX.eventBus().send(Eventbus.EXECUTE_QUERY, queryRequest);
                    }
                    else
                    {
                        LOGGER.error("Error in port check: " + asyncResult.cause().getMessage());
                    }
                });
    }
}
