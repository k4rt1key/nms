package org.nms.api;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import static org.nms.constants.Fields.ENDPOINTS.PROVISION_ENDPOINT;
import static org.nms.utils.ApiUtils.sendFailure;
import static org.nms.utils.ApiUtils.sendSuccess;
import static org.nms.validators.Validators.validateIpWithIpType;
import static org.nms.constants.Fields.DiscoveryCredential.DISCOVERY_ID;
import static org.nms.constants.Fields.PluginDiscoveryRequest.IPS;
import static org.nms.constants.Fields.PluginPollingRequest.METRIC_GROUPS;

import org.nms.App;
import org.nms.cache.MonitorCache;
import org.nms.constants.Config;
import org.nms.validators.Validators;
import org.nms.constants.Fields;
import org.nms.constants.Queries;
import org.nms.utils.DbUtils;

import java.util.ArrayList;
import java.util.List;

public class Provision implements AbstractHandler
{
    private static Provision instance;

    private Provision()
    {}

    public static Provision getInstance()
    {
        if(instance == null)
        {
            instance = new Provision();
        }

        return instance;
    }

    @Override
    public void init(Router router)
    {
        var provisionRouter = Router.router(App.vertx);

        provisionRouter.get("/")
                .handler(ctx -> AbstractHandler.list(ctx, Fields.Monitor.TABLE_NAME));

        provisionRouter.get("/:id")
                .handler(ctx -> AbstractHandler.get(ctx, Fields.Monitor.TABLE_NAME));

        provisionRouter.post("/")
                .handler(this::insert);

        provisionRouter.patch("/:id")
                .handler(this::update);

        provisionRouter.get("/")
                .handler(ctx -> AbstractHandler.delete(ctx, Fields.Monitor.TABLE_NAME));

        router.route(PROVISION_ENDPOINT).subRouter(provisionRouter);
    }

    @Override
    public void insert(RoutingContext ctx)
    {
        if(validateProvisionCreation(ctx)) { return; }

        var discoveryId = ctx.body().asJsonObject().getString(DISCOVERY_ID);

        var ips = ctx.body().asJsonObject().getJsonArray(IPS);

        DbUtils.sendQueryExecutionRequest(Queries.Discovery.GET_WITH_RESULTS_BY_ID, new JsonArray().add(Integer.parseInt(discoveryId))).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var discoveriesWithResult = asyncResult.result();

                if (discoveriesWithResult == null || discoveriesWithResult.isEmpty())
                {
                    sendFailure(ctx, 404, "Failed To Get Discovery with that id");

                    return;
                }

                if (discoveriesWithResult.getJsonObject(0).getString(Fields.Discovery.STATUS).equals(Fields.Discovery.PENDING_STATUS))
                {
                    sendFailure(ctx, 409, "Can't provision pending discovery");

                    return;
                }

                var results = discoveriesWithResult.getJsonObject(0).getJsonArray(Fields.Discovery.RESULT_JSON);

                var addMonitorsFuture = new ArrayList<Future>();

                if (results != null && !results.isEmpty())
                {
                    for (var j = 0; j < ips.size(); j++)
                    {
                        addMonitorsFuture.add(DbUtils.sendQueryExecutionRequest(Queries.Monitor.INSERT, new JsonArray().add(Integer.parseInt(discoveryId)).add(ips.getString(j))));
                    }

                    var joinFuture = CompositeFuture.join(addMonitorsFuture);

                    joinFuture.onComplete(provisionsInsertion ->
                    {
                        if (provisionsInsertion.succeeded())
                        {
                            var savedProvisions = provisionsInsertion.result();

                            var provisionArray = new JsonArray();

                            for (var i = 0; i < savedProvisions.size(); i++)
                            {
                                if (savedProvisions.resultAt(i) != null)
                                {
                                    var savedProvision = (JsonArray) savedProvisions.resultAt(i);

                                    if(!savedProvision.isEmpty())
                                    {
                                        provisionArray.add(savedProvision.getJsonObject(0));
                                    }
                                }
                            }

                            MonitorCache.getInstance().insert(provisionArray);

                            sendSuccess(ctx, 200, "Provisioned All Ips Which Was Correct, Ignoring Incorrect Ips", provisionArray);
                        }
                        else
                        {
                            sendFailure(ctx, 500, "Error during provisioning", provisionsInsertion.cause().getMessage());
                        }
                    });
                }
            }
            else
            {
                sendFailure(ctx, 400, "Discovery Not Found");
            }
        });
    }

    @Override
    public void delete(RoutingContext ctx)
    {
        var id = Validators.validateID(ctx);

        if( id == -1 ){ return; }

        DbUtils.sendQueryExecutionRequest(Queries.Monitor.GET_BY_ID, new JsonArray().add(id)).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var provision = asyncResult.result();

                if (provision.isEmpty())
                {
                    sendFailure(ctx, 404, "Provision not found");

                    return;
                }

                DbUtils.sendQueryExecutionRequest(Queries.Monitor.DELETE, new JsonArray().add(id)).onComplete(monitorDeletion ->
                {
                    if (monitorDeletion.succeeded())
                    {
                        var deletedMonitor = monitorDeletion.result();

                        if (!deletedMonitor.isEmpty())
                        {
                            MonitorCache.getInstance().delete(deletedMonitor.getJsonObject(0).getInteger(Fields.MetricGroup.ID));

                            sendSuccess(ctx, 200, "Provision deleted successfully", provision);
                        }
                        else
                        {
                            sendFailure(ctx, 400, "Failed to delete Provision");
                        }
                    }
                    else
                    {
                        sendFailure(ctx, 500, "Something Went Wrong", monitorDeletion.cause().getMessage());
                    }
                });
            }
            else
            {
                sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
            }
        });
    }

    @Override
    public void update(RoutingContext ctx)
    {
        if(validateProvisionUpdation(ctx)) { return; }

        var id = ctx.body().asJsonObject().getString(Fields.Monitor.ID);

        var metrics = ctx.body().asJsonObject().getJsonArray(METRIC_GROUPS);

        DbUtils.sendQueryExecutionRequest(Queries.Monitor.GET_BY_ID, new JsonArray().add(id))
                .compose(monitor ->
                {
                    if (monitor.isEmpty())
                    {
                        sendFailure(ctx, 404, "Monitor with id " + id + " Not exist");

                        return Future.failedFuture("Monitor with id " + id + " Not exist");
                    }

                    var updateMetricGroupsFuture = new ArrayList<Future<JsonArray>>();

                    for (var i = 0; i < metrics.size(); i++)
                    {
                        var name = metrics.getJsonObject(i).getString(Fields.MetricGroup.NAME);

                        var pollingInterval = metrics.getJsonObject(i).getInteger(Fields.MetricGroup.POLLING_INTERVAL);

                        var isEnabled = metrics.getJsonObject(i).getBoolean(Fields.MetricGroup.IS_ENABLED);

                        updateMetricGroupsFuture.add(DbUtils.sendQueryExecutionRequest(Queries.Monitor.UPDATE, new JsonArray().add(id).add(pollingInterval).add(name).add(isEnabled)));
                    }

                    return Future.join(updateMetricGroupsFuture);

                }).onComplete(asyncResult ->
                {
                    if (asyncResult.succeeded())
                    {
                        DbUtils.sendQueryExecutionRequest(Queries.Monitor.GET_BY_ID, new JsonArray().add(id)).onComplete(monitorResult ->
                        {
                            if (monitorResult.succeeded())
                            {
                                var res = monitorResult.result();

                                MonitorCache.getInstance().update(res.getJsonObject(0).getJsonArray(Fields.Monitor.METRIC_GROUP_JSON));

                                sendSuccess(ctx, 200, "Updated Provision", res);
                            }
                            else
                            {
                                sendFailure(ctx, 500, "Failed To Update Discovery", asyncResult.cause().getMessage());
                            }
                        });
                    }
                    else
                    {
                        sendFailure(ctx, 500, "Error Updating Metric groups", asyncResult.cause().getMessage());
                    }
                });
    }

    private boolean validateProvisionCreation(RoutingContext ctx)
    {
        final String IPS = "ips";

        final String DISCOVERY_ID = "discovery_id";

        if(Validators.validateBody(ctx)) { return false; }

        if(Validators.validateInputFields(ctx, new String[]{IPS, DISCOVERY_ID}, true)) { return false; }

        var body = ctx.body().asJsonObject();

        var ips = body.getJsonArray(IPS);

        var invalidIps = new StringBuilder();

        for(var ip: ips)
        {
            if(validateIpWithIpType(ip.toString(), "SINGLE"))
            {
                invalidIps.append(ip).append(", ");
            }
        }

        if(!invalidIps.isEmpty())
        {
            ctx.response().setStatusCode(400).end("Invalid IPs: " + invalidIps);

            return true;
        }

        if(body.getString(DISCOVERY_ID).isEmpty())
        {
            sendFailure(ctx, 400, "Discovery ID must be at least 1 character");

            try
            {
                Integer.parseInt(body.getString(DISCOVERY_ID));

                return false;
            }
            catch (Exception exception)
            {
                sendFailure(ctx, 400, "Discovery ID must be integer");

                return true;
            }
        }

        return false;
    }

    private boolean validateProvisionUpdation(RoutingContext ctx)
    {
        if(Validators.validateBody(ctx)) { return true; }

        var id = Validators.validateID(ctx);

        if( id == -1 ){ return true; }

        if(Validators.validateInputFields(ctx, new String[]{METRIC_GROUPS}, true)) { return true; }

        var body = ctx.body().asJsonObject();

        var metrics = body.getJsonArray(METRIC_GROUPS);

        if(metrics != null && !metrics.isEmpty())
        {
            for(var i = 0; i < metrics.size(); i++)
            {
                var type = metrics.getJsonObject(i).getString(Fields.MetricGroup.NAME);

                var metricType = new ArrayList<>(List.of("CPUINFO", "CPUUSAGE", "DISK", "MEMORY", "DISK", "UPTIME", "PROCESS", "NETWORK", "SYSTEMINFO"));

                if(!metricType.contains(type))
                {
                    sendFailure(ctx, 400,"Invalid Metric Type " + type);

                    return true;
                }

                var interval = metrics.getJsonObject(i).getInteger(Fields.MetricGroup.POLLING_INTERVAL);

                var enable = metrics.getJsonObject(i).getBoolean(Fields.MetricGroup.IS_ENABLED);

                if(type == null)
                {
                    sendFailure(ctx, 400,"Provide Valid Metric Type");

                    return true;
                }

                if( ( (interval == null || interval < Config.SCHEDULER_CHECKING_INTERVAL || interval % Config.SCHEDULER_CHECKING_INTERVAL != 0) & enable == null ) )
                {
                    sendFailure(ctx, 400,"Provide Polling Interval ( Multiple of " + Config.SCHEDULER_CHECKING_INTERVAL + " ) Or Enable ( true or false )");

                    return true;
                }

            }
        }

        return false;
    }
}