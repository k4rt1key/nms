package org.nms.HttpServer.Controllers;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.nms.App;
import org.nms.Cache.MetricGroupCacheStore;
import org.nms.ConsoleLogger;
import org.nms.HttpServer.Utility.HttpResponse;

import java.util.ArrayList;
import java.util.List;

public class ProvisionController
{
    public static void getAllProvisions(RoutingContext ctx)
    {
        App.provisionService
                .getAll()
                .onSuccess(provisions ->
                {
                    // Discoveries not found
                    if (provisions.isEmpty())
                    {
                        HttpResponse.sendFailure(ctx, 404, "No provisions found");
                        return;
                    }

                    // Discoveries found
                    HttpResponse.sendSuccess(ctx, 200, "Provisions found", provisions);
                })
                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));

    }

    public static void getProvisionById(RoutingContext ctx)
    {
        var id = Integer.parseInt(ctx.request().getParam("id"));

        App.provisionService
                .get(new JsonArray().add(id))
                .onSuccess(provision ->
                {
                    // Discovery not found
                    if (provision.isEmpty())
                    {
                        HttpResponse.sendFailure(ctx, 404, "Provision not found");
                        return;
                    }

                    // Discovery found
                    HttpResponse.sendSuccess(ctx, 200, "Provision found", provision);
                })
                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));

    }

    public static void createProvision(RoutingContext ctx)
    {
        var discovery_id = Integer.parseInt(ctx.body().asJsonObject().getString("discovery_id"));
        var ips = ctx.body().asJsonObject().getJsonArray("ips");
        App.discoveryService
                .getWithResultsById(new JsonArray().add(discovery_id))
                .onSuccess(discoveriesWithResult ->
                {
                    if(discoveriesWithResult == null || discoveriesWithResult.isEmpty() )
                    {
                        HttpResponse.sendFailure(ctx, 404, "Failed To Get Discovery with that id");
                        return;
                    }

                    if(discoveriesWithResult.getJsonObject(0).getString("status").equals("PENDING"))
                    {
                        HttpResponse.sendFailure(ctx, 409, "Can't provision pending discovery");
                        return;
                    }
                    var results = discoveriesWithResult.getJsonObject(0).getJsonArray("results");

                    // Check if user sent wrong Ips
                    var wrongIps = new ArrayList<String>();


                    List<Future> provisionsToAddFutures = new ArrayList<>();

                    if(results != null && results.size() > 0)
                    {
                        for(int j = 0; j < ips.size(); j++){

                            var isAnyWrongIp = true;
                            for(int i = 0; i < results.size(); i++) {
                            {
                                if (results.getJsonObject(i).getString("status").equals("COMPLETED"))
                                {
                                    if(ips.getString(j).equals(results.getJsonObject(i).getString("ip"))) {
                                        isAnyWrongIp = false;
                                        provisionsToAddFutures.add(App.provisionService.save(new JsonArray().add(discovery_id).add(ips.getString(j))));
                                    }
                                }

                            }
                            if(isAnyWrongIp)
                            {
                                wrongIps.add(ips.getString(j));
                                isAnyWrongIp = true;
                            }
                        }

                    }

                    CompositeFuture.join(provisionsToAddFutures)
                            .onSuccess(v -> {
                                v.onSuccess(savedProvisions -> {
                                    var provisionedIps = new JsonArray();
                                    for(var i = 1; i < savedProvisions.size() + 1; i++)
                                    {
                                        if(savedProvisions.resultAt(0) != null)
                                        {
                                            provisionedIps.add(savedProvisions.resultAt(0));
                                        }
                                    }


                                    for(var i = 0; i < provisionedIps.size(); i++) {
                                        var provisionedObject = provisionedIps.getJsonObject(i);

                                        ConsoleLogger.debug("Inside DEBUG");

                                        for(var k = 0; k < provisionedObject.getJsonArray("metric_groups").size(); k++) {
                                            // Create a NEW JsonObject for each metric group
                                            var metricGroupValue = new JsonObject()
                                                    .put("provision_profile_id", provisionedObject.getInteger("id"))
                                                    .put("port", Integer.parseInt(provisionedObject.getString("port")))
                                                    // Make a copy of the credential object to avoid reference issues
                                                    .put("credential", provisionedObject.getJsonObject("credential").copy())
                                                    .put("ip", provisionedObject.getString("ip"))
                                                    .put("name", provisionedObject.getJsonArray("metric_groups").getJsonObject(k).getString("name"))
                                                    .put("polling_interval", provisionedObject.getJsonArray("metric_groups").getJsonObject(k).getInteger("polling_interval"));

                                            var key = provisionedObject.getJsonArray("metric_groups").getJsonObject(k).getInteger("id");
                                            ConsoleLogger.debug("Adding metric group with ID: " + key + " and name: " +
                                                    provisionedObject.getJsonArray("metric_groups").getJsonObject(k).getString("name"));

                                            MetricGroupCacheStore.setCachedMetricGroup(key, metricGroupValue);
                                            MetricGroupCacheStore.setReferencedMetricGroup(key, metricGroupValue);
                                        }
                                    }

                                    HttpResponse.sendSuccess(ctx, 200, "Provisioned All Valid Ips", provisionedIps);

                                });
                            })
                            .onFailure(err -> {
                                err.printStackTrace();
                                HttpResponse.sendFailure(ctx, 500, "Error during provisioning", err.getMessage());
                            });
                }
                })
                .onFailure(err -> HttpResponse.sendFailure(ctx, 400, "Discovery Not Found", ""));

    }

    public static void deleteProvision(RoutingContext ctx)
    {
        var id = Integer.parseInt(ctx.request().getParam("id"));

        // First check if discovery exists
        App.provisionService
                .delete(new JsonArray().add(id))
                .onSuccess(provision -> {
                    if (provision.isEmpty())
                    {
                        HttpResponse.sendFailure(ctx, 404, "Provision not found");
                        return;
                    }

                    // Discovery found, proceed with delete
                    App.discoveryService
                            .delete(new JsonArray().add(id))
                            .onSuccess(deletedDiscovery -> HttpResponse.sendSuccess(ctx, 200, "Provision deleted successfully", provision))
                            .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));
                })
                .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Something Went Wrong", err.getMessage()));

    }

    public static void updateMetric(RoutingContext ctx)
    {
        var id = Integer.parseInt(ctx.request().getParam("id"));

        var metrics = ctx.body().asJsonObject().getJsonArray("metrics");

        List<Future<JsonArray>> updateMetricGroupsFuture = new ArrayList<>();

        for(var i = 0; i < metrics.size(); i++)
        {
            var name = metrics.getJsonObject(i).getString("name");
            var pollingInterval = metrics.getJsonObject(i).getInteger("polling_interval");
            var enable = metrics.getJsonObject(i).getBoolean("enable");

            updateMetricGroupsFuture.add(App.provisionService.update(new JsonArray().add(id).add(pollingInterval).add(name)));
        }

        Future.join(updateMetricGroupsFuture)
                .onFailure(err -> {
                    HttpResponse.sendFailure(ctx, 500, "Error Updating Metric groups", err.getMessage());
                })
                .onSuccess(v -> {
                   App.provisionService.get(new JsonArray().add(id))
                           .onSuccess(res -> {
                               HttpResponse.sendSuccess(ctx, 200, "Updated Provision", res);
                           })
                           .onFailure(err -> HttpResponse.sendFailure(ctx, 500, "Failed To Update Discovery", err.getMessage()));
                });
    }

}
