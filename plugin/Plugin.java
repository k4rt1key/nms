package org.nms.plugin;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import static org.nms.App.LOGGER;
import org.nms.constants.Config;
import org.nms.constants.Fields;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Plugin extends AbstractVerticle
{
    @Override
    public void start()
    {
        vertx.eventBus().<JsonObject>localConsumer(Fields.EventBus.PLUGIN_SPAWN_ADDRESS, message ->
        {
            var request = message.body();

            spawnPlugin(request)
                    .onComplete(pluginResponse -> message.reply(pluginResponse.result()));
        });

        LOGGER.info("✅ Plugin Verticle Deployed");
    }


    @Override
    public void stop()
    {
        LOGGER.info("⚠ Plugin Verticle Stopped");
    }

    /**
     * Spawns plugin, Sends request and returns Json Response
     */
    private Future<JsonObject> spawnPlugin(JsonObject request)
    {
        return vertx.executeBlocking(() ->
        {
            try
            {
                // Calculate timeout based on request type
                var timeout = calculateTimeout(request);

                var encodedRequest = request.encode();

                // Prepare go command
                var goCommand = new String[] {Config.PLUGIN_PATH, encodedRequest};

                LOGGER.debug("Spawning plugin with request: " + encodedRequest);

                var builder = new ProcessBuilder(goCommand);

                // Run command
                var process = builder.start();

                // Wait for response with timeout
                var doneExecuting = process.waitFor(timeout, TimeUnit.SECONDS);

                // If Timeout
                if (!doneExecuting)
                {
                    LOGGER.warn("⚠ Plugin is not responding within " + timeout + " seconds, process is being terminated !");

                    process.destroyForcibly();

                    // Send empty response
                    return new JsonObject();
                }

                // Else Read output
                var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                var output = reader.lines().collect(Collectors.joining());

                LOGGER.debug("Plugin response: " + output);

                return new JsonObject(output);
            }
            catch (Exception exception)
            {
                LOGGER.error("❌ Error spawning plugin: " + exception.getMessage());

                // Send empty response
                return new JsonObject();
            }
        });
    }

    /**
     * Calculates timeout based on request type and content
     */
    private int calculateTimeout(JsonObject request)
    {
        var type = request.getString("type", "");

        // Calculate timeout for discovery
        if ("discovery".equals(type))
        {
            var ips = request.getJsonArray(Fields.PluginDiscoveryRequest.IPS, null);

            var ipsCount = (ips != null) ? ips.size() : 0;

            return Config.BASE_TIME + (ipsCount * Config.DISCOVERY_TIMEOUT_PER_IP);
        }
        // Calculate timeout for polling
        else if ("polling".equals(type))
        {
            var metricGroups = request.getJsonArray(Fields.PluginPollingRequest.METRIC_GROUPS, null);

            var metricGroupCount = (metricGroups != null) ? metricGroups.size() : 0;

            return Config.BASE_TIME + (metricGroupCount * Config.POLLING_TIMEOUT_PER_METRIC_GROUP);
        }

        // Default timeout
        return Config.BASE_TIME;
    }
}