package org.nms.PluginManager;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nms.App;
import org.nms.ConsoleLogger;
import org.nms.Constants;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class PluginManager
{
    public static Future<JsonArray> runDiscovery(int discoveryId, JsonArray ips, int port, JsonArray credentials) {
        return App.vertx.executeBlocking(() -> {
            try {
                // Build input JSON
                JsonObject discoveryInput = new JsonObject();
                discoveryInput.put("type", "discovery");
                discoveryInput.put("id", discoveryId);
                discoveryInput.put("ips", ips);
                discoveryInput.put("port", port);
                discoveryInput.put("credentials", credentials);

                ConsoleLogger.debug("Discovery input " + discoveryInput);
                // Convert to command string
                String inputJsonStr = discoveryInput.encode();
                String[] command = {Constants.PLUGIN_PATH, inputJsonStr};

                System.out.println("Executing: " + String.join(" ", command));

                ProcessBuilder builder = new ProcessBuilder(command);
                builder.redirectErrorStream(true);
                Process process = builder.start();

                // Read stdout from Go program
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String output = reader.lines().collect(Collectors.joining());

                process.waitFor();

                System.out.println("Received output:\n" + output);

                // Parse output JSON
                JsonObject outputJson = new JsonObject(output);

                // Return results array
                return outputJson.getJsonArray("result");

            }
            catch (Exception e)
            {
                ConsoleLogger.error(e.getMessage());
                return new JsonArray();
            }
        }).onSuccess(Future::succeededFuture).onFailure(Future::failedFuture);
    }

    public static Future<JsonArray> runPolling(JsonArray metricGroups) {
        return App.vertx.executeBlocking(() -> {
            try {
                ConsoleLogger.debug("Received polling request for metric groups: " + metricGroups);

                // Build input JSON
                JsonObject pollingInput = new JsonObject();
                pollingInput.put("type", "polling");
                pollingInput.put("metric_groups", metricGroups);

                // Convert to command string
                String inputJsonStr = pollingInput.encode();
                String[] command = {Constants.PLUGIN_PATH, inputJsonStr};

                System.out.println("Executing polling: " + String.join(" ", command));

                ProcessBuilder builder = new ProcessBuilder(command);
                builder.redirectErrorStream(true);
                Process process = builder.start();

                // Read stdout from Go program
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String output = reader.lines().collect(Collectors.joining());

                process.waitFor();

                System.out.println("Received polling output:\n" + output);

                // Parse output JSON
                JsonObject outputJson = new JsonObject(output);

                // Return metric groups array
                return outputJson.getJsonArray("metric_groups");
            }
            catch (Exception e)
            {
                ConsoleLogger.error("Error executing polling: " + e.getMessage());
                return new JsonArray();
            }
        }).onSuccess(Future::succeededFuture).onFailure(Future::failedFuture);
    }

}