package org.nms.discovery;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nms.App;
import org.nms.ConsoleLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class PingCheck
{
    private static final int TIMEOUT_SECONDS = 30; // Process timeout

    public static Future<JsonArray> pingIps(JsonArray ipArray)
    {
        return App.vertx.executeBlocking(promise ->
        {
            var results = new JsonArray();
            Process process = null;
            BufferedReader reader = null;

            try
            {
                ConsoleLogger.info("Received Pinging Request for " + ipArray);

                // Input validation
                if (ipArray == null || ipArray.isEmpty())
                {
                    ConsoleLogger.warn("Empty IP array provided for ping check");
                    promise.complete(results);
                    return;
                }

                // Validate IP addresses and collect valid ones
                var validIps = new JsonArray();
                for (int i = 0; i < ipArray.size(); i++)
                {
                    var ipObj = ipArray.getValue(i);

                    if (!(ipObj instanceof String))
                    {
                        ConsoleLogger.warn("Non-string IP address found, skipping: " + ipObj);
                        results.add(createErrorResult(String.valueOf(ipObj), "Invalid IP format"));
                        continue;
                    }

                    var ip = (String) ipObj;

                    if (ip.trim().isEmpty())
                    {
                        ConsoleLogger.warn("Empty IP address found, skipping");
                        results.add(createErrorResult("", "Empty IP address"));
                        continue;
                    }

                    validIps.add(ip);
                }

                if (validIps.isEmpty())
                {
                    ConsoleLogger.warn("No valid IP addresses found");
                    promise.complete(results);
                    return;
                }

                // Step - 1 : Prepare fping command [ fping -c3 ip1 ip2 ... ]
                var command = new String[validIps.size() + 2];
                command[0] = "fping";
                command[1] = "-c3";

                for (int i = 0; i < validIps.size(); i++)
                {
                    command[i + 2] = validIps.getString(i);
                }

                var pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                process = pb.start();

                // Set up a timeout for the process
                var completed = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (!completed)
                {
                    process.destroyForcibly();
                    throw new TimeoutException("fping process timed out after " + TIMEOUT_SECONDS + " seconds");
                }

                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;

                // Track processed IPs to identify any missing results
                var processedIps = new JsonArray();

                while ((line = reader.readLine()) != null)
                {
                    // Format: <ip> : xmt/rcv/%loss = 3/3/0%, min/avg/max = 2.34/4.56/6.78
                    var parts = line.split(":");
                    if (parts.length < 2) continue;

                    var ip = parts[0].trim();
                    var stats = parts[1].trim();
                    processedIps.add(ip);

                    var result = new JsonObject().put("ip", ip);

                    if (stats.contains("100%"))
                    {
                        result.put("message", "Ping check failed: 100% packet loss");
                        result.put("success", false);
                    }
                    else
                    {
                        String avg = getString(stats);
                        result.put("message", "Ping check success (avg latency: " + avg + ")");
                        result.put("success", true);
                    }

                    results.add(result);
                }

                // Handle any IPs that didn't get processed (no output from fping)
                for (var i = 0; i < validIps.size(); i++)
                {
                    var ip = validIps.getString(i);
                    if (!processedIps.contains(ip))
                    {
                        results.add(createErrorResult(ip, "No response from fping"));
                    }
                }

                promise.complete(results);
            }
            catch (TimeoutException te)
            {
                ConsoleLogger.error("Ping process timed out: " + te.getMessage());
                handleErrorForAllIps(ipArray, results, "Ping process timed out after " + TIMEOUT_SECONDS + " seconds");
                promise.complete(results);
            }
            catch (IOException ioe)
            {
                ConsoleLogger.error("IO Error during ping: " + ioe.getMessage());
                handleErrorForAllIps(ipArray, results, "IO Error: " + ioe.getMessage());
                promise.complete(results);
            }
            catch (InterruptedException ie)
            {
                Thread.currentThread().interrupt(); // Reset interrupted status
                ConsoleLogger.error("Ping process interrupted: " + ie.getMessage());
                handleErrorForAllIps(ipArray, results, "Process interrupted");
                promise.complete(results);
            }
            catch (Exception e)
            {
                ConsoleLogger.error("Error pinging IPs: " + e.getMessage());
                handleErrorForAllIps(ipArray, results, "Error: " + e.getMessage());
                promise.complete(results);
            }
            finally
            {
                // Clean up resources
                closeQuietly(reader);

                if (process != null && process.isAlive())
                {
                    try
                    {
                        process.destroyForcibly();
                    }
                    catch (Exception e)
                    {
                        ConsoleLogger.error("Error destroying ping process: " + e.getMessage());
                    }
                }
            }
        });
    }

    private static String getString(String stats) {
        String avg = "";
        if (stats.contains("min/avg/max"))
        {
            String[] latencyParts = stats.split("min/avg/max = ");
            if (latencyParts.length == 2)
            {
                String[] latencyValues = latencyParts[1].split("/");
                if (latencyValues.length >= 2)
                {
                    avg = latencyValues[1] + " ms";
                }
            }
        }
        return avg;
    }

    private static JsonObject createErrorResult(String ip, String message)
    {
        return new JsonObject()
                .put("ip", ip)
                .put("message", message)
                .put("success", false);
    }

    private static void handleErrorForAllIps(JsonArray ipArray, JsonArray results, String errorMessage)
    {
        if (ipArray != null)
        {
            for (int i = 0; i < ipArray.size(); i++)
            {
                String ip = String.valueOf(ipArray.getValue(i));
                boolean exists = false;

                for (int j = 0; j < results.size(); j++)
                {
                    JsonObject result = results.getJsonObject(j);
                    if (result.getString("ip", "").equals(ip))
                    {
                        exists = true;
                        break;
                    }
                }

                if (!exists)
                {
                    results.add(createErrorResult(ip, errorMessage));
                }
            }
        }
    }

    private static void closeQuietly(AutoCloseable closeable)
    {
        if (closeable != null)
        {
            try
            {
                closeable.close();
            }
            catch (Exception e)
            {
                ConsoleLogger.error("Error closing resource: " + e.getMessage());
            }
        }
    }

    private static class TimeoutException extends Exception
    {
        public TimeoutException(String message)
        {
            super(message);
        }
    }
}
