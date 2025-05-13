package org.nms.api.handlers;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Tuple;
import org.nms.Logger;
import org.nms.api.helpers.HttpResponse;
import org.nms.api.helpers.Ip;
import org.nms.constants.Fields;
import org.nms.constants.Queries;
import org.nms.database.helpers.DbEventBus;

import java.util.ArrayList;
import java.util.List;

import static org.nms.App.vertx;

public class DiscoveryHandler
{
    public static void getAllDiscoveries(RoutingContext ctx)
    {
       DbEventBus.sendQueryExecutionRequest(Queries.Discovery.GET_ALL)
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

        DbEventBus.sendQueryExecutionRequest(Queries.Discovery.GET_BY_ID,  new JsonArray().add(id))
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

        DbEventBus.sendQueryExecutionRequest(Queries.Discovery.GET_WITH_RESULTS_BY_ID, new JsonArray().add(id))
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
        DbEventBus.sendQueryExecutionRequest(Queries.Discovery.GET_ALL_WITH_RESULTS)
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
        DbEventBus.sendQueryExecutionRequest(Queries.Discovery.INSERT ,new JsonArray().add(name).add(ip).add(ipType).add(port))
                .onSuccess(discovery ->
                {
                    // !!!! Discovery Not Created
                    if (discovery.isEmpty())
                    {
                        HttpResponse.sendFailure(ctx, 400, "Failed to create discovery");
                        return;
                    }

                    // Step 2: Add credentials to discovery
                    List<Tuple> credentialsToAdd = new ArrayList<>();

                    var discoveryId = discovery.getJsonObject(0).getInteger(Fields.Discovery.ID);

                    for (int i = 0; i < credentials.size(); i++)
                    {
                        var credentialId = Integer.parseInt(credentials.getString(i));

                        credentialsToAdd.add(Tuple.of(discoveryId, credentialId));
                    }

                    // Save Credentials Into DB
                    DbEventBus.sendQueryExecutionRequest(Queries.Discovery.INSERT_CREDENTIAL, credentialsToAdd)
                            .onSuccess(discoveryCredentials ->
                            {
                                // !!! Credentials Not Created
                                if (discoveryCredentials.isEmpty())
                                {
                                    HttpResponse.sendFailure(ctx, 400, "Failed to add credentials to discovery");
                                    return;
                                }

                                // Step 3: Get complete discovery with credentials
                                DbEventBus.sendQueryExecutionRequest(Queries.Discovery.GET_BY_ID, new JsonArray().add(discoveryId))
                                        .onSuccess(discoveryWithCredentials -> HttpResponse.sendSuccess(ctx, 201, "Discovery created successfully", discoveryWithCredentials))
                                        .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
                            })
                            .onFailure(err ->
                            {
                                // Rollback discovery creation if adding credentials fails
                                DbEventBus.sendQueryExecutionRequest(Queries.Discovery.DELETE, new JsonArray().add(discoveryId))
                                        .onFailure(rollbackErr -> Logger.error("Failed to rollback discovery creation: " + rollbackErr.getMessage()));

                                HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage());
                            });
                })
                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
    }

    public static void updateDiscovery(RoutingContext ctx)
    {
        var id = Integer.parseInt(ctx.request().getParam("id"));

        // First check if discovery exists
        DbEventBus.sendQueryExecutionRequest(Queries.Discovery.GET_BY_ID, new JsonArray().add(id))
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
                    DbEventBus.sendQueryExecutionRequest(Queries.Discovery.UPDATE, new JsonArray().add(id).add(name).add(ip).add(ipType).add(port))
                            .onSuccess(updatedDiscovery ->
                            {
                                // !!! Discovery Not Updated
                                if (updatedDiscovery.isEmpty())
                                {
                                    HttpResponse.sendFailure(ctx, 400, "Failed to update discovery");
                                    return;
                                }

                                // Get updated discovery with credentials
                                DbEventBus.sendQueryExecutionRequest(Queries.Discovery.GET_BY_ID, new JsonArray().add(id))
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
        DbEventBus.sendQueryExecutionRequest(Queries.Discovery.GET_BY_ID, new JsonArray().add(id))
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
                    List<Tuple> credentialsToAdd = new ArrayList<>();

                    if (addCredentials != null && !addCredentials.isEmpty()) {
                        for (int i = 0; i < addCredentials.size(); i++)
                        {
                            var credentialId = Integer.parseInt(addCredentials.getString(i));
                            credentialsToAdd.add(Tuple.of(id, credentialId));
                        }

                        DbEventBus.sendQueryExecutionRequest(Queries.Discovery.INSERT_CREDENTIAL, credentialsToAdd)
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

            DbEventBus.sendQueryExecutionRequest(Queries.Discovery.DELETE_CREDENTIAL, credentialsToRemove)
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
        DbEventBus.sendQueryExecutionRequest(Queries.Discovery.GET_BY_ID, new JsonArray().add(discoveryId))
                .onSuccess(updatedDiscovery -> HttpResponse.sendSuccess(ctx, 200, "Discovery credentials updated successfully", updatedDiscovery))
                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
    }

    public static void deleteDiscovery(RoutingContext ctx)
    {
        var id = Integer.parseInt(ctx.request().getParam("id"));

        // First check if discovery exists
        DbEventBus.sendQueryExecutionRequest(Queries.Discovery.DELETE, new JsonArray().add(id))
                .onSuccess(discovery ->
                {
                    if (discovery.isEmpty())
                    {
                        HttpResponse.sendFailure(ctx, 404, "Discovery not found");
                        return;
                    }

                    // Discovery found, proceed with delete
                    DbEventBus.sendQueryExecutionRequest(Queries.Discovery.GET_BY_ID, new JsonArray().add(id))
                            .onSuccess(deletedDiscovery -> HttpResponse.sendSuccess(ctx, 200, "Discovery deleted successfully", discovery))
                            .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
                })
                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
    }

    public static void runDiscovery(RoutingContext ctx)
    {
        try
        {
            // Step 1: Extract and validate ID
            int id = Integer.parseInt(ctx.request().getParam("id"));

            // Step 2: Check if discovery exists
            DbEventBus.sendQueryExecutionRequest(Queries.Discovery.GET_BY_ID, new JsonArray().add(id))
                    .onSuccess(discovery ->
                    {
                        if (discovery.isEmpty())
                        {
                            HttpResponse.sendFailure(ctx, 404, "Discovery not found");
                            return;
                        }

                        // Step 3: Send discovery run request via event bus to the Discovery
                        vertx.eventBus().request(
                                Fields.EventBus.RUN_DISCOVERY_ADDRESS,
                                new JsonObject().put("id", id)
                        );

                        // Step 4: Return immediate success response to the client
                        HttpResponse.sendSuccess(ctx, 202, "Discovery request with id " + id + " accepted and is being processed",
                                new JsonArray());
                    })
                    .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
        }
        catch (Exception e)
        {
            HttpResponse.sendFailure(ctx, 400, "Invalid discovery ID provided");
        }
    }
}