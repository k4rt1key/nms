package org.nms.discovery;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClientOptions;
import org.nms.App;
import org.nms.ConsoleLogger;

import java.net.UnknownHostException;
import java.util.ArrayList;

public class PortCheck
{

    public static Future<JsonArray> checkPorts(JsonArray ips, int port)
    {
        Promise<JsonArray> allDone = Promise.promise();

        var results = new JsonArray();
        var futures = new ArrayList<Future>();

        if (ips == null || ips.isEmpty())
        {
            ConsoleLogger.warn("Empty IP list provided for port check");
            allDone.complete(results);
            return allDone.future();
        }

        if (port < 1 || port > 65535)
        {
            ConsoleLogger.error("Invalid port number: " + port);
            allDone.fail(new IllegalArgumentException("Port must be between 1 and 65535"));
            return allDone.future();
        }

        try
        {
            for (var ipObj : ips)
            {
                if (!(ipObj instanceof String))
                {
                    ConsoleLogger.warn("Non-string IP address found, skipping: " + ipObj);
                    continue;
                }

                String ip = (String) ipObj;

                if (ip.trim().isEmpty())
                {
                    ConsoleLogger.warn("Empty IP address found, skipping");
                    continue;
                }

                var checkPromise = Promise.promise();

                futures.add(checkPromise.future());

                var result = new JsonObject().put("ip", ip).put("port", port);

                try
                {
                    var client = App.vertx.createNetClient(new NetClientOptions()
                            .setConnectTimeout(2000)
                            .setReconnectAttempts(0));

                    client.connect(port, ip, ar ->
                    {
                        try
                        {
                            if (ar.succeeded())
                            {
                                var socket = ar.result();

                                result.put("message", "Port " + port + " is open on " + ip);
                                result.put("success", true);

                                socket.close();
                            }
                            else
                            {
                                var cause = ar.cause();
                                var errorMessage = cause != null ? cause.getMessage() : "Unknown error";

                                if (errorMessage.contains("Connection refused"))
                                {
                                    result.put("message", "Port " + port + " is closed on " + ip);
                                }
                                else if (errorMessage.contains("timed out"))
                                {
                                    result.put("message", "Port " + port + " connection timed out on " + ip);
                                }
                                else if (cause instanceof UnknownHostException)
                                {
                                    result.put("message", "Unknown host: " + ip);
                                }
                                else
                                {
                                    result.put("message", errorMessage);
                                }
                                result.put("success", false);
                            }
                        }
                        catch (Exception e)
                        {
                            ConsoleLogger.error("Error handling connection result for " + ip + ":" + port + " - " + e.getMessage());

                            result.put("message", "Error during connection handling: " + e.getMessage());
                            result.put("success", false);
                        }
                        finally
                        {
                            results.add(result);
                            checkPromise.complete(result);
                            try
                            {
                                client.close();
                            }
                            catch (Exception e)
                            {
                                ConsoleLogger.error("Error closing NetClient: " + e.getMessage());
                            }
                        }
                    });
                }
                catch (Exception e)
                {
                    ConsoleLogger.error("Failed to create or connect client for " + ip + ":" + port + " - " + e.getMessage());

                    result.put("message", "Error creating connection: " + e.getMessage());
                    result.put("success", false);
                    results.add(result);

                    checkPromise.complete(result);
                }
            }

            CompositeFuture.all(new ArrayList<>(futures))
                    .onComplete(ar ->
                    {
                        if (ar.succeeded())
                        {
                            allDone.complete(results);
                        }
                        else
                        {
                            ConsoleLogger.error("Error in port check batch operation: " + ar.cause().getMessage());
                            allDone.complete(results);
                        }
                    });
        }
        catch (Exception e)
        {
            ConsoleLogger.error("Unexpected error in checkPorts: " + e.getMessage());
            allDone.fail(e);
        }

        return allDone.future();
    }
}
