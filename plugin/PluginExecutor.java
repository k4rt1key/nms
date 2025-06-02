package org.nms.plugin;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nms.constants.Eventbus;
import org.nms.constants.Global;
import org.nms.constants.Plugin;

import static org.nms.App.LOGGER;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class PluginExecutor extends AbstractVerticle
{
    @Override
    public void start()
    {
        vertx.eventBus().localConsumer(Eventbus.SPAWN_PLUGIN, this::spawnPlugin);

        LOGGER.info("Plugin Verticle Deployed");
    }

    @Override
    public void stop()
    {
        LOGGER.info("Plugin Verticle Stopped");
    }

    /**
     * Spawns plugin, Sends request and returns Json Response
     */
    private void spawnPlugin(Message<JsonObject> message)
    {

        var request = message.body();

        var type = request.getString(Plugin.Common.TYPE);

        // Calculate timeout based on request type
        var timeout = calculateTimeout(request);

        var encodedRequest = request.encode();

        // Prepare go command
        var goCommand = new String[] { Global.PLUGIN_PATH, encodedRequest};

        LOGGER.debug("Spawning plugin with request: " + encodedRequest);

        vertx.executeBlocking(() ->
        {
            try
            {
                var builder = new ProcessBuilder(goCommand);

                // Run command
                var process = builder.start();

                long timerId = vertx.setTimer(timeout * 1000L, (tId)->
                {
                    if(process.isAlive())
                    {
                        LOGGER.info("Timeout for proccess " + process.pid() + ", terminating...");

                        process.destroyForcibly();
                    }
                });

                // Else Read output
                var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                var response = new JsonArray();

                String line;

                while ((line = reader.readLine()) != null)
                {
                    try
                    {
                        response.add(new JsonObject(line));
                    }
                    catch (Exception e)
                    {
                        LOGGER.warn("Error Parsing Json: " + line);
                    }

                    LOGGER.debug("Plugin response: " + line);
                }

                reader.close();

                if(!vertx.timer(timerId).isComplete())
                {
                    vertx.timer(timerId).cancel();
                }

                if(type.equals(Plugin.DiscoveryRequest.DISCOVERY))
                {
                    vertx.eventBus().send(Eventbus.SAVE_CREDENTIAL_CHECK, response);
                }
                else if(type.equals(Plugin.PollingRequest.POLLING))
                {
                    vertx.eventBus().send(Eventbus.SAVE_CREDENTIAL_CHECK, response);
                }
            }
            catch (Exception exception)
            {
                LOGGER.error("Error spawning plugin: " + exception.getMessage());

                // Send empty response
                return new JsonArray();
            }

            return null;
        });
    }

    /**
     * Calculates timeout based on request type and content
     */
    private int calculateTimeout(JsonObject request)
    {
        var type = request.getString(Plugin.Common.TYPE, "");

        if (Plugin.Common.DISCOVERY.equals(type))
        {
            var ips = request.getJsonArray(Plugin.DiscoveryRequest.IPS, null);

            var ipsCount = (ips != null) ? ips.size() : 0;

            return Global.BASE_PLUGIN_TIMEOUT + (ipsCount * Global.DISCOVERY_TIMEOUT_PER_IP);
        }

        else if (Plugin.Common.POLLING.equals(type))
        {
            var metricGroups = request.getJsonArray(Plugin.PollingRequest.METRIC_GROUPS, null);

            var metricGroupCount = (metricGroups != null) ? metricGroups.size() : 0;

            return Global.BASE_PLUGIN_TIMEOUT + (metricGroupCount * Global.POLLING_TIMEOUT_PER_METRIC);
        }

        // Default timeout
        return Global.BASE_PLUGIN_TIMEOUT;
    }
}