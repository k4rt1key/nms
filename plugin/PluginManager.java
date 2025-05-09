package org.nms.plugin;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.nms.ConsoleLogger;
import org.nms.constants.Config;
import org.nms.constants.Fields;
import static org.nms.App.vertx;

public class PluginManager
{
    private static final int INITIAL_OVERHEAD = 5;

    private static final int DISCOVERY_TIMEOUT_PER_IP = 1;

    private static final int POLLING_TIMEOUT_PER_METRIC_GROUP = 1;

    public static Future<JsonArray> runDiscovery(int discoveryId, JsonArray ips, int port, JsonArray credentials)
    {
        return vertx.executeBlocking(() ->
        {
            try
            {
                var DISCOVERY_TIMEOUT = INITIAL_OVERHEAD + ( ips.size() * DISCOVERY_TIMEOUT_PER_IP);
                // Step-1.1 : Prepare Request Json
                JsonObject discoveryInput = new JsonObject();
                discoveryInput.put(Fields.PluginDiscoveryRequest.TYPE, Fields.PluginDiscoveryRequest.DISCOVERY);
                discoveryInput.put(Fields.PluginDiscoveryRequest.ID, discoveryId);
                discoveryInput.put(Fields.PluginDiscoveryRequest.IPS, ips);
                discoveryInput.put(Fields.PluginDiscoveryRequest.PORT, port);
                discoveryInput.put(Fields.PluginDiscoveryRequest.CREDENTIALS, credentials);

                // Step-1.2 : Prepare Command
                String inputJsonStr = discoveryInput.encode();
                String[] command = {Config.PLUGIN_PATH, inputJsonStr};

                // Step-2 : Run Command
                var builder = new ProcessBuilder(command);
                builder.redirectErrorStream(true);
                var process = builder.start();

                // Waiting For Reply
                var done = process.waitFor(DISCOVERY_TIMEOUT, TimeUnit.SECONDS);

                if(!done)
                {
                    ConsoleLogger.warn("⏱️ GoPlugin Is Not Responding Within " + DISCOVERY_TIMEOUT + " Seconds");

                    process.destroyForcibly();

                    return new JsonArray();
                }
                else
                {
                    // Step-3 : Read Output From Go's Stream
                    var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    var output = reader.lines().collect(Collectors.joining());

                    // Parse Response JSON
                    var outputJson = new JsonObject(output);

                    // Return Result Array
                    return outputJson.getJsonArray("result");
                }

            }
            catch (Exception e)
            {
                ConsoleLogger.error("❌ Error Running Discovery In PluginManager " + e.getMessage());

                return new JsonArray();
            }
        });
    }

    public static Future<JsonArray> runPolling(JsonArray metricGroups)
    {
        return vertx.executeBlocking(() ->
        {
            try
            {
                var POLLING_TIMEOUT = INITIAL_OVERHEAD + ( metricGroups.size() * POLLING_TIMEOUT_PER_METRIC_GROUP );

                // Step-1.1 : Prepare Request Json
                JsonObject pollingInput = new JsonObject();

                pollingInput.put(Fields.PluginPollingRequest.TYPE, Fields.PluginPollingRequest.DISCOVERY);
                pollingInput.put(Fields.PluginPollingRequest.METRIC_GROUPS, metricGroups);

                // Step-1.2 : Prepare Command
                String inputJsonStr = pollingInput.encode();
                String[] command = {Config.PLUGIN_PATH, inputJsonStr};

                ConsoleLogger.debug("Sending " + Arrays.toString(command));

                // Step-2 : Run Command
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.redirectErrorStream(true);
                Process process = builder.start();

                // Waiting 20 Seconds For Reply
                boolean done = process.waitFor(POLLING_TIMEOUT, TimeUnit.SECONDS);

                if(!done)
                {
                    ConsoleLogger.warn("⏱️ GoPlugin Is Not Responding Within " + POLLING_TIMEOUT + " Seconds");

                    return new JsonArray();
                }
                else
                {
                    // Step-3 : Read Output From Go's Stream
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String output = reader.lines().collect(Collectors.joining());

                    // Parse Response JSON
                    JsonObject outputJson = new JsonObject(output);

                    ConsoleLogger.debug("Recieved " + outputJson.encode());

                    // Return Result Array
                    return outputJson.getJsonArray(Fields.PluginPollingResponse.METRIC_GROUPS);
                }
            }
            catch (Exception e)
            {
                ConsoleLogger.error("❌ Error Running Discovery In PluginManager " + e.getMessage());

                return new JsonArray();
            }
        });
    }

}