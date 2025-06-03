package org.nms.api;

import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.nms.constants.DatabaseQueries;

import static org.nms.App.VERTX;
import static org.nms.utils.ApiUtils.sendFailure;

public class PollingResult implements AbstractHandler
{
    private static PollingResult instance;

    private PollingResult(){}

    public static PollingResult getInstance()
    {
        if(instance == null)
        {
            instance =  new PollingResult();
        }

        return instance;
    }

    @Override
    public void init(Router router)
    {
        var ENDPOINT = "/api/v1/polling_result/*";

        var pollingResultRouter = Router.router(VERTX);

        router.get("/").handler((ctx) -> this.list(ctx, DatabaseQueries.UserProfile.LIST));

        router.route(ENDPOINT).subRouter(pollingResultRouter);
    }


    @Override
    public void insert(RoutingContext ctx)
    {
        sendFailure(ctx, 501, "Method not implemented");
    }

    @Override
    public void update(RoutingContext ctx)
    {
        sendFailure(ctx, 501, "Method not implemented");
    }

    @Override
    public void updateCache(JsonArray data)
    {
        // NA
    }

    @Override
    public void insertCache(JsonArray data)
    {
        // NA
    }

    @Override
    public void deleteCache(JsonArray data)
    {
        // NA
    }
}