package org.nms.api;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import org.nms.App;
import org.nms.constants.Database;
import org.nms.constants.Fields;
import org.nms.utils.DbUtils;

import static org.nms.App.VERTX;
import static org.nms.constants.Eventbus.EXECUTE_QUERY;
import static org.nms.constants.Eventbus.RUN_DISCOVERY;
import static org.nms.constants.Fields.ENDPOINTS.DISCOVERY_ENDPOINT;
import static org.nms.utils.ApiUtils.sendFailure;
import static org.nms.utils.ApiUtils.sendSuccess;

public class Discovery implements BaseHandler
{
    private static Discovery instance;

    private Discovery(){}

    public static Discovery getInstance()
    {
        if(instance == null)
        {
            instance = new Discovery();
        }

        return instance;
    }

    @Override
    public void init(Router router)
    {
        var discoveryRouter = Router.router(VERTX);

        discoveryRouter.get("/:id")
                .handler((ctx) -> this.get(ctx, Database.Table.DISCOVERY));

        discoveryRouter.get("/")
                .handler((ctx) -> this.list(ctx, Database.Table.DISCOVERY));

        discoveryRouter.post("/")
                .handler((ctx) -> this.insert(ctx, Database.Table.DISCOVERY));

        discoveryRouter.post("/run/:id")
                .handler((ctx) -> this.run(ctx, Database.Table.DISCOVERY));

        discoveryRouter.patch("/:id")
                .handler((ctx) -> this.update(ctx, Database.Table.DISCOVERY));

        discoveryRouter.delete("/:id")
                .handler((ctx) -> this.delete(ctx, Database.Table.DISCOVERY));

        router.route(DISCOVERY_ENDPOINT).subRouter(discoveryRouter);
    }

    @Override
    public void beforeInsert(RoutingContext ctx)
    {

    }

    @Override
    public void afterInsert(JsonArray data)
    {

    }

    @Override
    public void beforeUpdate(RoutingContext ctx)
    {

    }

    @Override
    public void afterUpdate(JsonArray data)
    {

    }

    public void run(RoutingContext ctx, String tableName)
    {
        try
        {
            var conditions = new JsonArray();

            var params = ctx.pathParams();

            for(String key : params.keySet())
            {
                conditions.add(new JsonObject().put(key, params.get(key)));
            }

            var queryRequest = DbUtils.buildRequest(
                    tableName,
                    Database.Operation.GET,
                    conditions,
                    null
            );

            App.VERTX.eventBus().<JsonArray>request(EXECUTE_QUERY, queryRequest, dbResponse ->
            {
                if (dbResponse.succeeded())
                {
                    var discovery = dbResponse.result().body();

                    if (discovery.isEmpty())
                    {
                        sendFailure(ctx, 404, "Discovery not found");

                        return;
                    }

                    var id = discovery.getJsonObject(0).getInteger(Fields.Discovery.ID);

                    VERTX.eventBus().send (
                            RUN_DISCOVERY,
                            new JsonObject().put(Fields.Discovery.ID, id)
                    );

                    sendSuccess(ctx, 202, "Discovery request with id " + id + " accepted and is being processed", new JsonArray());
                }
                else
                {
                    sendFailure(ctx, 500, "Something Went Wrong", dbResponse.cause().getMessage());
                }
            });
        }
        catch (Exception exception)
        {
            sendFailure(ctx, 400, "Invalid discovery ID provided");
        }
    }

}