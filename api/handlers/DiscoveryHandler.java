package org.nms.api.handlers;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Tuple;
import org.nms.ConsoleLogger;
import org.nms.api.helpers.HttpResponse;
import org.nms.api.helpers.Ip;
import org.nms.constants.Fields;
import org.nms.constants.Queries;
import org.nms.database.DbEngine;
import org.nms.discovery.PingCheck;
import org.nms.discovery.PortCheck;
import org.nms.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryHandler
{
    public static void getAllDiscoveries(RoutingContext ctx)
    {
       DbEngine.execute(Queries.Discovery.GET_ALL)
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
        var id = Integer.parseInt(ctx.request().getParam("id"));

        DbEngine.execute(Queries.Discovery.GET_BY_ID,  new JsonArray().add(id))
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
        var id = Integer.parseInt(ctx.request().getParam("id"));

        DbEngine.execute(Queries.Discovery.GET_WITH_RESULTS_BY_ID, new JsonArray().add(id))
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
        DbEngine.execute(Queries.Discovery.GET_ALL_WITH_RESULTS)
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
        DbEngine.execute(Queries.Discovery.INSERT ,new JsonArray().add(name).add(ip).add(ipType).add(port))
                .onSuccess(discovery ->
                {
                    // !!!! Discovery Not Created
                    if (discovery.isEmpty())
                    {
                        HttpResponse.sendFailure(ctx, 400, "Failed to create discovery");
                        return;
                    }

                    // Step 2: Add credentials to discovery
                    var credentialsToAdd = new ArrayList<Tuple>();

                    var discoveryId = discovery.getJsonObject(0).getInteger(Fields.Discovery.ID);

                    for (int i = 0; i < credentials.size(); i++)
                    {
                        var credentialId = Integer.parseInt(credentials.getString(i));

                        credentialsToAdd.add(Tuple.of(discoveryId, credentialId));
                    }

                    // Save Credentials Into DB
                    DbEngine.execute(Queries.Discovery.INSERT_CREDENTIAL, credentialsToAdd)
                            .onSuccess(discoveryCredentials ->
                            {
                                // !!! Credentials Not Created
                                if (discoveryCredentials.isEmpty())
                                {
                                    HttpResponse.sendFailure(ctx, 400, "Failed to add credentials to discovery");
                                    return;
                                }

                                // Step 3: Get complete discovery with credentials
                                DbEngine.execute(Queries.Discovery.GET_BY_ID, new JsonArray().add(discoveryId))
                                        .onSuccess(discoveryWithCredentials -> HttpResponse.sendSuccess(ctx, 201, "Discovery created successfully", discoveryWithCredentials))
                                        .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
                            })
                            .onFailure(err ->
                            {
                                // Rollback discovery creation if adding credentials fails
                                DbEngine.execute(Queries.Discovery.DELETE, new JsonArray().add(discoveryId))
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
        DbEngine.execute(Queries.Discovery.GET_BY_ID, new JsonArray().add(id))
                .onSuccess(discovery ->
                {
                    // Step-1: Check if discovery Exist
                    if (discovery.isEmpty())
                    {
                        HttpResponse.sendFailure(ctx, 404, "Discovery not found");
                        return;
                    }

                    // Discovery Already run
                    if(discovery.getJsonObject(0).getString(Fields.Discovery.STATUS).equals(Fields.DiscoveryResult.COMPLETED_STATUS))
                    {
                        HttpResponse.sendFailure(ctx, 400, "Discovery Already Run");
                        return;
                    }

                    // Discovery found, proceed with update
                    var name = ctx.body().asJsonObject().getString("name");

                    var ip = ctx.body().asJsonObject().getString("ip");

                    var ipType = ctx.body().asJsonObject().getString("ip_type");

                    var port = ctx.body().asJsonObject().getInteger("port");

                    // Update Discovery
                    DbEngine.execute(Queries.Discovery.UPDATE, new JsonArray().add(id).add(name).add(ip).add(ipType).add(port))
                            .onSuccess(updatedDiscovery ->
                            {
                                // !!! Discovery Not Updated
                                if (updatedDiscovery.isEmpty())
                                {
                                    HttpResponse.sendFailure(ctx, 400, "Failed to update discovery");
                                    return;
                                }

                                // Get updated discovery with credentials
                                DbEngine.execute(Queries.Discovery.GET_BY_ID, new JsonArray().add(id))
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

        // Step-1: Check if discovery exists
        DbEngine.execute(Queries.Discovery.GET_BY_ID, new JsonArray().add(id))
                .onSuccess(discovery ->
                {
                    // !!! Discovery Not Found
                    if (discovery.isEmpty())
                    {
                        HttpResponse.sendFailure(ctx, 404, "Discovery not found");
                        return;
                    }

                    var addCredentials = ctx.body().asJsonObject().getJsonArray("add_credentials");

                    var removeCredentials = ctx.body().asJsonObject().getJsonArray("remove_credentials");

                    // Step 2: Add credentials to discovery if any
                    var credentialsToAdd = new ArrayList<Tuple>();

                    if (addCredentials != null && !addCredentials.isEmpty()) {
                        for (var i = 0; i < addCredentials.size(); i++)
                        {
                            var credentialId = Integer.parseInt(addCredentials.getString(i));
                            credentialsToAdd.add(Tuple.of(id, credentialId));
                        }

                        DbEngine.execute(Queries.Discovery.INSERT_CREDENTIAL, credentialsToAdd)
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
           var credentialsToRemove = new ArrayList<Tuple>();

            for (var i = 0; i < removeCredentials.size(); i++)
            {
                var credentialId = Integer.parseInt(removeCredentials.getString(i));
                credentialsToRemove.add(Tuple.of(discoveryId, credentialId));
            }

            DbEngine.execute(Queries.Discovery.DELETE_CREDENTIAL, credentialsToRemove)
                    .onSuccess(v ->
                    {
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
        DbEngine.execute(Queries.Discovery.GET_BY_ID, new JsonArray().add(discoveryId))
                .onSuccess(updatedDiscovery -> HttpResponse.sendSuccess(ctx, 200, "Discovery credentials updated successfully", updatedDiscovery))
                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
    }

    public static void deleteDiscovery(RoutingContext ctx)
    {
        var id = Integer.parseInt(ctx.request().getParam("id"));

        // First check if discovery exists
        DbEngine.execute(Queries.Discovery.DELETE, new JsonArray().add(id))
                .onSuccess(discovery ->
                {
                    if (discovery.isEmpty())
                    {
                        HttpResponse.sendFailure(ctx, 404, "Discovery not found");
                        return;
                    }

                    // Discovery found, proceed with delete
                    DbEngine.execute(Queries.Discovery.GET_BY_ID, new JsonArray().add(id))
                            .onSuccess(deletedDiscovery -> HttpResponse.sendSuccess(ctx, 200, "Discovery deleted successfully", discovery))
                            .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
                })
                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
    }

    public static void runDiscovery(RoutingContext ctx)
    {
        var id = Integer.parseInt(ctx.request().getParam("id"));

        // Step 1: Check if discovery exists
        DbEngine.execute(Queries.Discovery.GET_BY_ID, new JsonArray().add(id))
                .compose(discoveryWithCredentials ->
                {
                    if (discoveryWithCredentials.isEmpty())
                    {
                        HttpResponse.sendFailure(ctx, 404, "Discovery not found");
                        return Future.failedFuture(new Exception("Discovery not found"));
                    }

                    DbEngine
                            .execute(Queries.Discovery.DELETE_RESULT, new JsonArray().add(id))
                            .compose(v -> {


                                // Extract necessary information
                                var ips = discoveryWithCredentials.getJsonObject(0).getString(Fields.Discovery.IP);
                                var ipType = discoveryWithCredentials.getJsonObject(0).getString(Fields.Discovery.IP_TYPE);
                                var port = discoveryWithCredentials.getJsonObject(0).getInteger(Fields.Discovery.PORT);

                                var ipArray = Ip.getIpListAsJsonArray(ips, ipType);

                                // Step 2.1: Ping Check - Direct implementation instead of eventbus
                                PingCheck.pingIps(ipArray)
                                        .compose(pingResults ->
                                        {
                                            var pingCheckPassedIps = new JsonArray();

                                            var pingCheckFailedIps = new JsonArray();

                                            // Process ping results
                                            for (int i = 0; i < pingResults.size(); i++) {
                                                var result = pingResults.getJsonObject(i);

                                                var success = result.getBoolean("success");

                                                if (success) {
                                                    pingCheckPassedIps.add(result.getString("ip"));
                                                } else {
                                                    pingCheckFailedIps.add(result);
                                                }
                                            }

                                            // If There is FailedIps then insert them into the discovery result
                                            List<Tuple> ipsToAdd = new ArrayList<>();

                                            if (!pingCheckFailedIps.isEmpty()) {
                                                for (var i = 0; i < pingCheckFailedIps.size(); i++) {
                                                    var ip = pingCheckFailedIps.getJsonObject(i).getString("ip");

                                                    var message = pingCheckFailedIps.getJsonObject(i).getString("message");

                                                    ipsToAdd.add(Tuple.of(id, null, ip, message, "FAILED"));
                                                }

                                                return DbEngine
                                                        .execute(Queries.Discovery.INSERT_RESULT, ipsToAdd)
                                                        .compose(v2 -> Future.succeededFuture(pingCheckPassedIps));
                                            } else {
                                                return Future.succeededFuture(pingCheckPassedIps);
                                            }
                                        })
                                        .compose(pingCheckPassedIps -> PortCheck.checkPorts(pingCheckPassedIps, port))
                                        .compose(portCheckResults ->
                                        {
                                            // Step 2.2: Port Check
                                            var portCheckPassedIps = new JsonArray();

                                            var portCheckFailedIps = new JsonArray();

                                            // Process ping results
                                            for (int i = 0; i < portCheckResults.size(); i++) {
                                                var result = portCheckResults.getJsonObject(i);

                                                var success = result.getBoolean("success");

                                                if (success)
                                                {
                                                    portCheckPassedIps.add(result.getString("ip"));
                                                }
                                                else
                                                {
                                                    portCheckFailedIps.add(result);
                                                }
                                            }

                                            // If There is FailedIps then insert them into the discovery result
                                            List<Tuple> ipsToAdd = new ArrayList<>();

                                            if (!portCheckFailedIps.isEmpty()) {
                                                for (var i = 0; i < portCheckFailedIps.size(); i++) {
                                                    var ip = portCheckFailedIps.getJsonObject(i).getString("ip");

                                                    var message = portCheckFailedIps.getJsonObject(i).getString("message");

                                                    ipsToAdd.add(Tuple.of(id, null, ip, message, "FAILED"));
                                                }

                                                return DbEngine
                                                        .execute(Queries.Discovery.INSERT_RESULT, ipsToAdd)
                                                        .compose(v2 -> Future.succeededFuture(portCheckPassedIps));
                                            } else {
                                                return Future.succeededFuture(portCheckPassedIps);
                                            }
                                        })
                                        .compose(portCheckPassedIps ->
                                        {
                                            // Step 3: Credentials Check

                                            // Get Ips
                                            var ipsToCheckForCredentials = new JsonArray();

                                            for (var i = 0; i < portCheckPassedIps.size(); i++) {
                                                ipsToCheckForCredentials.add(portCheckPassedIps.getString(i));
                                            }

                                            // Get Credentials
                                            var credentialsToCheck = discoveryWithCredentials.getJsonObject(0).getJsonArray(Fields.Discovery.CREDENTIAL_JSON);

                                            return PluginManager
                                                    .runDiscovery(id, ipsToCheckForCredentials, port, credentialsToCheck)
                                                    .compose((credentialCheckResults ->
                                                    {
                                                        var credentialCheckPassedResult = new JsonArray();

                                                        var credentialCheckFailedResult = new JsonArray();

                                                        for (var i = 0; i < credentialCheckResults.size(); i++) {
                                                            var status = credentialCheckResults.getJsonObject(i).getBoolean("success");

                                                            if (status) {
                                                                credentialCheckPassedResult.add(credentialCheckResults.getJsonObject(i));
                                                            } else {
                                                                credentialCheckFailedResult.add(credentialCheckResults.getJsonObject(i));
                                                            }
                                                        }

                                                        // Handle failed credential checks
                                                        var ipsToAddAfterCredentialFailure = new ArrayList<Tuple>();

                                                        if (!credentialCheckFailedResult.isEmpty()) {
                                                            for (var i = 0; i < credentialCheckFailedResult.size(); i++) {
                                                                var ip = credentialCheckFailedResult.getJsonObject(i).getString(Fields.PluginDiscoveryResponse.IP);

                                                                var message = credentialCheckFailedResult.getJsonObject(i).getString(Fields.PluginDiscoveryResponse.MESSAGE);

                                                                ipsToAddAfterCredentialFailure.add(Tuple.of(id, null, ip, message, "FAILED"));
                                                            }
                                                        }

                                                        // Handle successful credential checks
                                                        var ipsToAddAfterCredentialSuccess = new ArrayList<Tuple>();

                                                        if (!credentialCheckPassedResult.isEmpty()) {
                                                            for (var i = 0; i < credentialCheckPassedResult.size(); i++) {
                                                                var ip = credentialCheckPassedResult.getJsonObject(i).getString(Fields.PluginDiscoveryResponse.IP);

                                                                var message = credentialCheckPassedResult.getJsonObject(i).getString(Fields.PluginDiscoveryResponse.MESSAGE);

                                                                var credential = credentialCheckPassedResult.getJsonObject(i).getJsonObject(Fields.PluginDiscoveryResponse.CREDENTIALS).getInteger(Fields.Credential.ID);

                                                                ipsToAddAfterCredentialSuccess.add(Tuple.of(id, credential, ip, message, "COMPLETED"));
                                                            }
                                                        }

                                                        // Handle empty lists to avoid batch query errors
                                                       var failureFuture = ipsToAddAfterCredentialFailure.isEmpty()
                                                                ? Future.succeededFuture(new JsonArray())
                                                                : DbEngine.execute(Queries.Discovery.INSERT_RESULT, ipsToAddAfterCredentialFailure);

                                                        var successFuture = ipsToAddAfterCredentialSuccess.isEmpty()
                                                                ? Future.succeededFuture(new JsonArray())
                                                                : DbEngine.execute(Queries.Discovery.INSERT_RESULT, ipsToAddAfterCredentialSuccess);

                                                        return Future.join(failureFuture, successFuture);
                                                    }));
                                        })
                                        // Step 5: Get complete discovery with credentials
                                        .compose(v2 -> DbEngine.execute(Queries.Discovery.UPDATE_STATUS, new JsonArray().add(id).add("COMPLETED")))
                                        // Step 6 : Get discovery with result
                                        .compose(v2 -> DbEngine.execute(Queries.Discovery.GET_WITH_RESULTS_BY_ID, new JsonArray().add(id))
                                                .onSuccess(response -> HttpResponse.sendSuccess(ctx, 200, "Discovery run successfully", response))
                                                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage())));

                                return Future.succeededFuture();
                            });
                    return Future.succeededFuture();
                })
                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
    }
}