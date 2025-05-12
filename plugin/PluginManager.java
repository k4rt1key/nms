package org.nms.plugin;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nms.ConsoleLogger;
import org.nms.constants.Config;
import org.nms.constants.Fields;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PluginManager extends AbstractVerticle
{
    private static final int INITIAL_OVERHEAD = 5;
    private static final int DISCOVERY_TIMEOUT_PER_IP = 1;
    private static final int POLLING_TIMEOUT_PER_METRIC_GROUP = 10;

    private static final String PLUGIN_TYPE_DISCOVERY = "discovery";
    private static final String PLUGIN_TYPE_POLLING = "polling";

    @Override
    public void start()
    {
        // Discovery Handler
        vertx.eventBus().consumer(Fields.EventBus.DISCOVERY_ADDRESS, message ->
        {
            var body = (JsonObject) message.body();
            runDiscovery(
                    body.getInteger(Fields.PluginDiscoveryRequest.ID),
                    body.getJsonArray(Fields.PluginDiscoveryRequest.IPS),
                    body.getInteger(Fields.PluginDiscoveryRequest.PORT),
                    body.getJsonArray(Fields.PluginDiscoveryRequest.CREDENTIALS)
            ).onComplete(ar -> message.reply(ar.result()));
        });

        // Polling Handler
        vertx.eventBus().consumer(Fields.EventBus.POLLING_ADDRESS, message ->
        {
            var body = (JsonObject) message.body();
            runPolling(body.getJsonArray(Fields.PluginPollingRequest.METRIC_GROUPS))
                    .onComplete(ar -> message.reply(ar.result()));
        });
    }

    private Future<JsonArray> runDiscovery(int discoveryId, JsonArray ips, int port, JsonArray credentials)
    {
        return vertx.executeBlocking(() ->
        {
            try
            {
                var DISCOVERY_TIMEOUT = INITIAL_OVERHEAD + (ips.size() * DISCOVERY_TIMEOUT_PER_IP);

                // Prepare Discovery Input
                var discoveryInput = new JsonObject()
                        .put("type", PLUGIN_TYPE_DISCOVERY)
                        .put("id", discoveryId)
                        .put("ips", ips)
                        .put("port", port)
                        .put("credentials", credentials);

                var inputJsonStr = discoveryInput.encode();
                var command = new String[] {Config.PLUGIN_PATH, inputJsonStr};

                // Run Discovery Command
                var builder = new ProcessBuilder(command);

                builder.redirectErrorStream(true);

                var process = builder.start();

                // Wait for Response
                var done = process.waitFor(DISCOVERY_TIMEOUT, TimeUnit.SECONDS);

                if (!done)
                {
                    ConsoleLogger.warn("GoPlugin not responding within " + DISCOVERY_TIMEOUT + " seconds");
                    process.destroyForcibly();
                    return new JsonArray();
                }

                // Read Output
                var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                var output = reader.lines().collect(Collectors.joining());

                // Parse Response
                var outputJson = new JsonObject(output);
                return outputJson.getJsonArray("result");
            }
            catch (Exception e)
            {
                ConsoleLogger.error("Error in Discovery: " + e.getMessage());
                return new JsonArray();
            }
        });
    }

    private Future<JsonArray> runPolling(JsonArray metricGroups)
    {
        return vertx.executeBlocking(() ->
        {
            try
            {
                var POLLING_TIMEOUT = INITIAL_OVERHEAD + (metricGroups.size() * POLLING_TIMEOUT_PER_METRIC_GROUP);

                // Prepare Polling Input
                var pollingInput = new JsonObject()
                        .put("type", PLUGIN_TYPE_POLLING)
                        .put(Fields.PluginPollingRequest.METRIC_GROUPS, metricGroups);

                var inputJsonStr = pollingInput.encode();
                var command = new String[] {Config.PLUGIN_PATH, inputJsonStr};

                ConsoleLogger.debug("Sending Polling Request: " + command[1]);

                // Run Polling Command
                var builder = new ProcessBuilder(command);
                builder.redirectErrorStream(true);
                var process = builder.start();

                // Wait for Response
                var done = process.waitFor(POLLING_TIMEOUT, TimeUnit.SECONDS);

                if (!done)
                {
                    ConsoleLogger.warn("GoPlugin not responding within " + POLLING_TIMEOUT + " seconds");
                    process.destroyForcibly();
                    return new JsonArray();
                }

                // Read Output
                var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                var output = reader.lines().collect(Collectors.joining());

                // Parse Response
                var outputJson = new JsonObject(output);

                ConsoleLogger.debug("Received Polling Response: " + outputJson.encode());

                // Return Results
                return outputJson.getJsonArray(Fields.PluginPollingRequest.METRIC_GROUPS);
            }
            catch (Exception e)
            {
                ConsoleLogger.error("Error in Polling: " + e.getMessage());
                return new JsonArray();
            }
        });
    }
}