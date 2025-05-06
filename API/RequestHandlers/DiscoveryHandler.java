package org.nms.API.RequestHandlers;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Tuple;
import org.nms.App;
import org.nms.ConsoleLogger;
import org.nms.API.Utility.HttpResponse;
import org.nms.API.Utility.IpHelpers;
import org.nms.PluginManager.PluginManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class DiscoveryHandler
{
    public static void getAllDiscoveries(RoutingContext ctx)
    {
        App.discoveryModel
                .getAllWithCredentials()
                .onSuccess(discoveries ->
                {
                    // Discoveries not found
                    if (discoveries.isEmpty())
                    {
                        HttpResponse.sendFailure(ctx, 404, "No discoveries found");
                        return;
                    }

                    // Discoveries found
                    HttpResponse.sendSuccess(ctx, 200, "Discoveries found", discoveries);
                })
                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
    }

    public static void getDiscoveryById(RoutingContext ctx)
    {
        int id = Integer.parseInt(ctx.request().getParam("id"));

        App.discoveryModel
                .getWithCredentialsById(new JsonArray().add(id))
                .onSuccess(discovery ->
                {
                    // Discovery not found
                    if (discovery.isEmpty())
                    {
                        HttpResponse.sendFailure(ctx, 404, "Discovery not found");
                        return;
                    }

                    // Discovery found
                    HttpResponse.sendSuccess(ctx, 200, "Discovery found", discovery);
                })
                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
    }

    public static void getDiscoveryResultsById(RoutingContext ctx)
    {
        int id = Integer.parseInt(ctx.request().getParam("id"));

        App.discoveryModel
                .getWithResultsById(new JsonArray().add(id))
                .onSuccess(discovery ->
                {
                    // Discovery not found
                    if (discovery.isEmpty())
                    {
                        HttpResponse.sendFailure(ctx, 404, "Discovery not found");
                        return;
                    }

                    // Discovery found
                    HttpResponse.sendSuccess(ctx, 200, "Discovery found", discovery);
                })
                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
    }

    public static void getDiscoveryResults(RoutingContext ctx)
    {
        App.discoveryModel
                .getAllWithResults()
                .onSuccess(discoveries ->
                {
                    // Discoveries not found
                    if (discoveries.isEmpty())
                    {
                        HttpResponse.sendFailure(ctx, 404, "No discoveries found");
                        return;
                    }

                    // Discoveries found
                    HttpResponse.sendSuccess(ctx, 200, "Discoveries found", discoveries);
                })
                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
    }

    public static void createDiscovery(RoutingContext ctx)
    {
        var name = ctx.body().asJsonObject().getString("name");

        var ip = ctx.body().asJsonObject().getString("ip");

        var ipType = ctx.body().asJsonObject().getString("ip_type");

        var credentials = ctx.body().asJsonObject().getJsonArray("credentials");

        var port = ctx.body().asJsonObject().getInteger("port");

        // Step 1: Create discovery
        App.discoveryModel
                .save(new JsonArray().add(name).add(ip).add(ipType).add(port))
                .onSuccess(discovery ->
                {
                    if (discovery.isEmpty())
                    {
                        HttpResponse.sendFailure(ctx, 400, "Failed to create discovery");
                        return;
                    }

                    // Step 2: Add credentials to discovery
                    List<Tuple> credentialsToAdd = new ArrayList<>();

                    var discoveryId = discovery.getJsonObject(0).getInteger("id");

                    for (int i = 0; i < credentials.size(); i++)
                    {
                        var credentialId = Integer.parseInt(credentials.getString(i));
                        credentialsToAdd.add(Tuple.of(discoveryId, credentialId));
                    }

                    App.discoveryModel
                            .saveCredentials(credentialsToAdd)
                            .onSuccess(discoveryCredentials ->
                            {
                                if (discoveryCredentials.isEmpty())
                                {
                                    HttpResponse.sendFailure(ctx, 400, "Failed to add credentials to discovery");
                                    return;
                                }

                                // Step 3: Get complete discovery with credentials
                                App.discoveryModel
                                        .getWithCredentialsById(new JsonArray().add(discoveryId))
                                        .onSuccess(discoveryWithCredentials -> HttpResponse.sendSuccess(ctx, 201, "Discovery created successfully", discoveryWithCredentials))
                                        .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
                            })
                            .onFailure(err ->
                            {
                                // Rollback discovery creation if adding credentials fails
                                App.discoveryModel
                                        .delete(new JsonArray().add(discoveryId))
                                        .onFailure(rollbackErr -> ConsoleLogger.error("Failed to rollback discovery creation: " + rollbackErr.getMessage()));

                                HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage());
                            });
                })
                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
    }

    public static void updateDiscovery(RoutingContext ctx)
    {
        var id = Integer.parseInt(ctx.request().getParam("id"));

        // First check if discovery exists
        App.discoveryModel
                .getWithCredentialsById(new JsonArray().add(id))
                .onSuccess(discovery -> {
                    if (discovery.isEmpty())
                    {
                        HttpResponse.sendFailure(ctx, 404, "Discovery not found");
                        return;
                    }

                    // Discovery found, proceed with update
                    var name = ctx.body().asJsonObject().getString("name");

                    var ip = ctx.body().asJsonObject().getString("ip");

                    var ipType = ctx.body().asJsonObject().getString("ip_type");

                    var port = ctx.body().asJsonObject().getInteger("port");

                    App.discoveryModel
                            .update(new JsonArray().add(id).add(name).add(ip).add(ipType).add(port))
                            .onSuccess(updatedDiscovery -> {
                                if (updatedDiscovery.isEmpty())
                                {
                                    HttpResponse.sendFailure(ctx, 400, "Failed to update discovery");
                                    return;
                                }

                                // Get updated discovery with credentials
                                App.discoveryModel
                                        .getWithCredentialsById(new JsonArray().add(id))
                                        .onSuccess(discoveryWithCredentials -> HttpResponse.sendSuccess(ctx, 200, "Discovery updated successfully", discoveryWithCredentials))
                                        .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
                            })
                            .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
                })
                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
    }

    public static void updateDiscoveryCredentials(RoutingContext ctx)
    {
        var id = Integer.parseInt(ctx.request().getParam("id"));

        // First check if discovery exists
        App.discoveryModel
                .getWithCredentialsById(new JsonArray().add(id))
                .onSuccess(discovery -> {
                    if (discovery.isEmpty())
                    {
                        HttpResponse.sendFailure(ctx, 404, "Discovery not found");
                        return;
                    }

                    var addCredentials = ctx.body().asJsonObject().getJsonArray("add_credentials");

                    var removeCredentials = ctx.body().asJsonObject().getJsonArray("remove_credentials");

                    // Step 1: Add credentials to discovery if any
                    List<Tuple> credentialsToAdd = new ArrayList<>();

                    if (addCredentials != null && !addCredentials.isEmpty()) {
                        for (int i = 0; i < addCredentials.size(); i++)
                        {
                            var credentialId = Integer.parseInt(addCredentials.getString(i));
                            credentialsToAdd.add(Tuple.of(id, credentialId));
                        }

                        App.discoveryModel
                                .saveCredentials(credentialsToAdd)
                                .onSuccess(addedCredentials ->
                                {
                                    // Step 2: Remove credentials if any
                                    processRemoveCredentials(ctx, id, removeCredentials);
                                })
                                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
                    }
                    else
                    {
                        // If no credentials to add, proceed to remove credentials
                        processRemoveCredentials(ctx, id, removeCredentials);
                    }
                })
                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
    }

    // Helper method to process removing credentials
    private static void processRemoveCredentials(RoutingContext ctx, int discoveryId, JsonArray removeCredentials)
    {
        if (removeCredentials != null && !removeCredentials.isEmpty())
        {
            List<Tuple> credentialsToRemove = new ArrayList<>();

            for (int i = 0; i < removeCredentials.size(); i++)
            {
                var credentialId = Integer.parseInt(removeCredentials.getString(i));
                credentialsToRemove.add(Tuple.of(discoveryId, credentialId));
            }

            App.discoveryModel
                    .deleteCredentials(credentialsToRemove)
                    .onSuccess(v -> {
                        // Return updated discovery with credentials
                        returnUpdatedDiscovery(ctx, discoveryId);
                    })
                    .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
        }
        else
        {
            // If no credentials to remove, just return the updated discovery
            returnUpdatedDiscovery(ctx, discoveryId);
        }
    }

    // Helper method to return updated discovery
    private static void returnUpdatedDiscovery(RoutingContext ctx, int discoveryId)
    {
        App.discoveryModel
                .getWithCredentialsById(new JsonArray().add(discoveryId))
                .onSuccess(updatedDiscovery -> {
                    HttpResponse.sendSuccess(ctx, 200, "Discovery credentials updated successfully", updatedDiscovery);
                })
                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
    }

    public static void deleteDiscovery(RoutingContext ctx)
    {
        var id = Integer.parseInt(ctx.request().getParam("id"));

        // First check if discovery exists
        App.discoveryModel
                .getWithCredentialsById(new JsonArray().add(id))
                .onSuccess(discovery -> {
                    if (discovery.isEmpty())
                    {
                        HttpResponse.sendFailure(ctx, 404, "Discovery not found");
                        return;
                    }

                    // Discovery found, proceed with delete
                    App.discoveryModel
                            .delete(new JsonArray().add(id))
                            .onSuccess(deletedDiscovery -> HttpResponse.sendSuccess(ctx, 200, "Discovery deleted successfully", discovery))
                            .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
                })
                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
    }

    public static void runDiscovery(RoutingContext ctx)
    {
        var id = Integer.parseInt(ctx.request().getParam("id"));

        // Step 1: Check if discovery exists
        App.discoveryModel.getWithCredentialsById(new JsonArray().add(id))
                .compose(discoveryWithCredentials ->
                {
                    if (discoveryWithCredentials.isEmpty())
                    {
                        HttpResponse.sendFailure(ctx, 404, "Discovery not found");
                        return Future.failedFuture(new Exception("Discovery not found"));
                    }

                    // Check if Discovery Status is PENDING
                    if(discoveryWithCredentials.getJsonObject(0).getString("status").equals("COMPLETED"))
                    {
                        return Future.failedFuture(new Exception("Discovery Already Run"));
                    }

                    if(discoveryWithCredentials.getJsonObject(0).getString("status").equals("COMPLETED"))
                    {
                        // Discovery is already complete
                        HttpResponse.sendFailure(ctx, 400, "Discovery is already complete");
                        return Future.failedFuture(new Exception("Discovery is already complete"));
                    }

                    // Extract necessary information
                    var ips = discoveryWithCredentials.getJsonObject(0).getString("ip");
                    var ipType = discoveryWithCredentials.getJsonObject(0).getString("ip_type");
                    var port = discoveryWithCredentials.getJsonObject(0).getInteger("port");
                    var ipArray = IpHelpers.getIpListAsJsonArray(ips, ipType);

                    // Step 2.1: Ping Check - Direct implementation instead of eventbus
                    return executeBlockingWithRetry(vertx -> IpHelpers.pingIps(ipArray))
                            .compose(pingResults ->
                            {
                                var passedIps = new JsonArray();
                                var failedIps = new JsonArray();

                                // Process ping results
                                for (int i = 0; i < pingResults.size(); i++)
                                {
                                    var result = pingResults.getJsonObject(i);
                                    var success = result.getBoolean("success");

                                    if (success)
                                    {
                                        passedIps.add(result.getString("ip"));
                                    }
                                    else
                                    {
                                        failedIps.add(result);
                                    }
                                }

                                // If failed IPs exist, insert them into the discovery result
                                List<Tuple> ipsToAdd = new ArrayList<>();

                                if (!failedIps.isEmpty()) {
                                    for (var i = 0; i < failedIps.size(); i++) {
                                        var ip = failedIps.getJsonObject(i).getString("ip");
                                        var message = failedIps.getJsonObject(i).getString("message");
                                        ipsToAdd.add(Tuple.of(id, ip, null, message, "FAIL"));
                                    }

                                    return App.discoveryModel.saveResults(ipsToAdd)
                                            .compose(v -> Future.succeededFuture(passedIps));
                                } else {
                                    return Future.succeededFuture(passedIps);
                                }
                            })
                            .compose(passedIps -> {
                                // Step 2.2: Port Check - Direct implementation instead of eventbus
                                List<Future<JsonObject>> portCheckFutures = new ArrayList<>();

                                for (int i = 0; i < passedIps.size(); i++) {
                                    String ip = passedIps.getString(i);
                                    portCheckFutures.add(
                                            executeBlockingWithRetry(vertx -> IpHelpers.checkPort(ip, port))
                                                    .otherwiseEmpty()
                                                    .recover(err -> Future.succeededFuture(new JsonObject()
                                                            .put("ip", ip)
                                                            .put("message", err.getMessage())
                                                            .put("success", false)))
                                    );
                                }

                                return Future.all(portCheckFutures)
                                        .map(compositeFuture -> {
                                            JsonArray portCheckResults = new JsonArray();
                                            for (int i = 0; i < compositeFuture.size(); i++) {
                                                JsonObject result = compositeFuture.resultAt(i);
                                                if (result != null) {
                                                    portCheckResults.add(result);
                                                }
                                            }
                                            return portCheckResults;
                                        });
                            })
                            .compose(portCheckResults -> {
                                var passedPortIps = new JsonArray();
                                var failedPortIps = new JsonArray();

                                for (int i = 0; i < portCheckResults.size(); i++) {
                                    var result = portCheckResults.getJsonObject(i);
                                    var success = result.getBoolean("success");

                                    if (success) {
                                        passedPortIps.add(result.getString("ip"));
                                    } else {
                                        failedPortIps.add(result);
                                    }
                                }

                                // If failed port IPs exist, insert them into the discovery result
                                List<Tuple> portCheckIpsToAdd = new ArrayList<>();

                                if (!failedPortIps.isEmpty()) {
                                    for (var i = 0; i < failedPortIps.size(); i++) {
                                        var ip = failedPortIps.getJsonObject(i).getString("ip");
                                        var message = failedPortIps.getJsonObject(i).getString("message");
                                        portCheckIpsToAdd.add(Tuple.of(id, ip, null, message, "FAIL"));
                                    }

                                    return App.discoveryModel.saveResults(portCheckIpsToAdd)
                                            .compose(v -> Future.succeededFuture(passedPortIps));
                                } else {
                                    return Future.succeededFuture(passedPortIps);
                                }
                            })
                            .compose(passedPortIps -> {
                                ConsoleLogger.info("Stage 3 reached");
                                // Step 3: Credentials Check

                                // Get Ips
                                var ipsToCheckForCredentials = new JsonArray();
                                for(var i = 0; i < passedPortIps.size(); i++)
                                {
                                    ipsToCheckForCredentials.add(passedPortIps.getString(i));
                                }

                                // Get Credentials
                                var credentialsToCheck = discoveryWithCredentials.getJsonObject(0).getJsonArray("credentials");

                                return PluginManager
                                        .runDiscovery(id, ipsToCheckForCredentials, port, credentialsToCheck)
                                        .compose((credentialCheckResArr -> {
                                            var credentialCheckPassedResult = new JsonArray();
                                            var credentialCheckFailedResult = new JsonArray();

                                            ConsoleLogger.debug("BHAI " + credentialCheckResArr);
                                            for(var i = 0; i < credentialCheckResArr.size(); i++)
                                            {
                                                var status = credentialCheckResArr.getJsonObject(i).getBoolean("success");

                                                if(status)
                                                {
                                                    credentialCheckPassedResult.add(credentialCheckResArr.getJsonObject(i));
                                                }
                                                else
                                                {
                                                    credentialCheckFailedResult.add(credentialCheckResArr.getJsonObject(i));
                                                }
                                            }

                                            // Handle failed credential checks
                                            var ipsToAddAfterCredentialFailure = new ArrayList<Tuple>();
                                            if (!credentialCheckFailedResult.isEmpty()) {
                                                for (var i = 0; i < credentialCheckFailedResult.size(); i++) {
                                                    var ip = credentialCheckFailedResult.getJsonObject(i).getString("ip");
                                                    var message = credentialCheckFailedResult.getJsonObject(i).getString("message");
                                                    ipsToAddAfterCredentialFailure.add(Tuple.of(id, ip, null, message, "FAIL"));
                                                }
                                            }

                                            // Handle successful credential checks
                                            var ipsToAddAfterCredentialSuccess = new ArrayList<Tuple>();
                                            if (!credentialCheckPassedResult.isEmpty()) {
                                                for (var i = 0; i < credentialCheckPassedResult.size(); i++) {
                                                    var ip = credentialCheckPassedResult.getJsonObject(i).getString("ip");
                                                    var message = credentialCheckPassedResult.getJsonObject(i).getString("message");
                                                    var credential = credentialCheckPassedResult.getJsonObject(i).getJsonObject("credential").getInteger("id");

                                                    ipsToAddAfterCredentialSuccess.add(Tuple.of(id, ip, credential, message, "COMPLETED"));
                                                }
                                            }

                                            // Handle empty lists to avoid batch query errors
                                            Future<JsonArray> failureFuture = ipsToAddAfterCredentialFailure.isEmpty()
                                                    ? Future.succeededFuture(new JsonArray())
                                                    : App.discoveryModel.saveResults(ipsToAddAfterCredentialFailure);

                                            Future<JsonArray> successFuture = ipsToAddAfterCredentialSuccess.isEmpty()
                                                    ? Future.succeededFuture(new JsonArray())
                                                    : App.discoveryModel.saveResults(ipsToAddAfterCredentialSuccess);

                                            return Future.join(failureFuture, successFuture);
                                        }));
                            })
                            .compose(v -> {
                                // Step 5: Get complete discovery with credentials
                                return App.discoveryModel.updateStatus(new JsonArray().add(id).add("COMPLETED"));
                            })
                            .compose(v -> App.discoveryModel.getWithResultsById(new JsonArray().add(id))
                                    .onSuccess(discoveryWithCredentialsAfterCheck -> HttpResponse.sendSuccess(ctx, 200, "Discovery run successfully", discoveryWithCredentialsAfterCheck))
                                    .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage())));
                })
                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
    }

    /**
     * Helper method to execute blocking operations with retry capability
     * @param operation The operation to execute
     * @param <T> The return type
     * @return A future with the operation result
     */
    private static <T> Future<T> executeBlockingWithRetry(Function<Vertx, T> operation) {
        Promise<T> promise = Promise.promise();

        App.vertx.executeBlocking(promise2 -> {
            try {
                T result = operation.apply(App.vertx);
                promise2.complete(result);
            } catch (Exception e) {
                promise2.fail(e);
            }
        }, false, ar -> {
            if (ar.succeeded()) {
                promise.complete((T) ar.result());
            } else {
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }
}