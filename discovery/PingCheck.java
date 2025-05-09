package org.nms.discovery;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nms.App;
import org.nms.ConsoleLogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class PingCheck
{
    public static Future<JsonArray> pingIps(JsonArray ipArray)
    {
        return App.vertx.executeBlocking(()->{

            ConsoleLogger.info("Recieved Pinging Request for " + ipArray);

            JsonArray results = new JsonArray();

            if (ipArray == null || ipArray.isEmpty())
            {
                return results;
            }

            try {
                // Step - 1 : Prepare fping command [ fping -c3 ip1 ip2 ... ]
                String[] command = new String[ipArray.size() + 2];
                command[0] = "fping";
                command[1] = "-c3";

                for (int i = 0; i < ipArray.size(); i++)
                {
                    command[i + 2] = ipArray.getString(i);
                }

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;

                while ((line = reader.readLine()) != null)
                {
                    // Format: <ip> : xmt/rcv/%loss = 3/3/0%, min/avg/max = 2.34/4.56/6.78
                    String[] parts = line.split(":");
                    if (parts.length < 2) continue;

                    String ip = parts[0].trim();
                    String stats = parts[1].trim();

                    JsonObject result = new JsonObject().put("ip", ip);

                    if (stats.contains("100%"))
                    {
                        result.put("message", "Ping check failed: 100% packet loss");
                        result.put("success", false);
                    }
                    else
                    {
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
                        result.put("message", "Ping check success (avg latency: " + avg + ")");
                        result.put("success", true);
                    }

                    results.add(result);
                }

                process.waitFor();
            }
            catch (Exception e)
            {
                for (int i = 0; i < ipArray.size(); i++)
                {
                    results.add(new JsonObject()
                            .put("ip", ipArray.getString(i))
                            .put("message", "Something Went Wrong"));

                    ConsoleLogger.error("Error Pinging Ips => " + e.getMessage());
                }
            }

            return results;
        });
    }
}
