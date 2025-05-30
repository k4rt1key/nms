package org.nms.api;

import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.nms.constants.Database;

import static org.nms.App.VERTX;
import static org.nms.constants.Fields.ENDPOINTS.DISCOVERY_RESULT_ENDPOINT;

public class DiscoveryResult implements BaseHandler
{
    private static DiscoveryResult instance;

    private DiscoveryResult()
    {

    }

    public static DiscoveryResult getInstance()
    {
        if(instance == null)
        {
            instance = new DiscoveryResult();
        }

        return instance;
    }

    @Override
    public void init(Router router)
    {
        var credentialRouter = Router.router(VERTX);

        credentialRouter.get("/")
                .handler((ctx) -> this.list(ctx, Database.Table.DISCOVERY_RESULT));

        credentialRouter.get("/:id")
                .handler((ctx) -> this.get(ctx, Database.Table.DISCOVERY_RESULT));

        credentialRouter.post("/")
                .handler((ctx) -> this.insert(ctx, Database.Table.DISCOVERY_RESULT));

        credentialRouter.patch("/:id")
                .handler((ctx) -> this.update(ctx, Database.Table.DISCOVERY_RESULT));

        credentialRouter.delete("/:id")
                .handler((ctx) -> this.delete(ctx, Database.Table.DISCOVERY_RESULT));

        router.route(DISCOVERY_RESULT_ENDPOINT).subRouter(credentialRouter);
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

}