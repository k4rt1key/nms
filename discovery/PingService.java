package org.nms.discovery;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nms.constants.Database;
import org.nms.constants.Eventbus;
import org.nms.constants.Global;
import org.nms.utils.DbUtils;
import org.nms.utils.DiscoveryUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import static org.nms.App.LOGGER;

public class PingService extends AbstractVerticle
{
    @Override
    public void start()
    {
        vertx.eventBus().localConsumer(Eventbus.PING_CHECK_START, this::pingIps);
    }

    @Override
    public void stop()
    {
        LOGGER.info("PingService Verticle Stopped");
    }

    private void pingIps(Message<JsonObject> message)
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
        vertx.executeBlocking((Promise<Void> promise) ->
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

                    vertx.eventBus().send(Eventbus.EXECUTE_QUERY, queryRequest);

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

                if(!failedResult.isEmpty()) vertx.eventBus().send(Eventbus.EXECUTE_QUERY, queryRequest);

                LOGGER.debug("ping check passed ips: " + successfulIps.encode());

                vertx.eventBus().send(Eventbus.PORT_CHECK_START, new JsonObject()
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
}
