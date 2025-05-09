package org.nms.discovery;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import org.nms.App;

import java.util.ArrayList;
import java.util.List;

public class PortCheck
{

    public static Future<JsonArray> checkPorts(JsonArray ips, int port)
    {

        Promise<JsonArray> allDone = Promise.promise();

        JsonArray results = new JsonArray();

        List<Future> futures = new ArrayList<>();

        for (var ip : ips)
        {
            // Create a promise for each IP check
            Promise<JsonObject> checkPromise = Promise.promise();
            futures.add(checkPromise.future());

            JsonObject result = new JsonObject().put("ip", ip).put("port", port);

            // Create NetClient with configuration
            NetClient client = App.vertx.createNetClient(new NetClientOptions()
                    .setConnectTimeout(2000)  // 2 second timeout
                    .setReconnectAttempts(0)); // No reconnection attempts

            // Attempt connection
            client.connect(port, (String) ip, ar ->
            {
                if (ar.succeeded()) {
                    // Connection successful - port is open
                    NetSocket socket = ar.result();

                    result.put("message", "Port " + port + " is open on " + ip);
                    result.put("success", true);

                    socket.close();
                }
                else
                {
                    // Connection failed - port is closed or unreachable
                    String errorMessage = ar.cause().getMessage();

                    if (errorMessage.contains("Connection refused"))
                    {
                        result.put("message", "Port " + port + " is closed on " + ip);
                    }
                    else if (errorMessage.contains("timed out"))
                    {
                        result.put("message", "Port " + port + " connection timed out on " + ip);
                    }
                    else
                    {
                        result.put("message", errorMessage);
                    }

                    result.put("success", false);
                }

                // Add the result to our array
                results.add(result);

                // Complete this IP's promise
                checkPromise.complete(result);

                // Always close the client to release resources
                client.close();
            });
        }

        // When all futures complete, resolve the main promise with all results
        CompositeFuture.all(new ArrayList<>(futures))
                .onComplete(ar ->
                {
                    if (ar.succeeded()) {
                        allDone.complete(results);
                    }
                    else
                    {
                        allDone.fail(ar.cause());
                    }
                });

        return allDone.future();
    }
}