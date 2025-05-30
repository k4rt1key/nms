package org.nms.api;

import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import static org.nms.constants.Fields.ENDPOINTS.PROVISION_ENDPOINT;

import org.nms.App;
import org.nms.constants.Database;

public class Monitor implements BaseHandler
{
    private static Monitor instance;

    private Monitor()
    {

    }

    public static Monitor getInstance()
    {
        if(instance == null)
        {
            instance = new Monitor();
        }

        return instance;
    }

    @Override
    public void init(Router router)
    {
        var provisionRouter = Router.router(App.VERTX);

        provisionRouter.get("/")
                .handler((ctx) -> this.list(ctx, Database.Table.MONITOR) );

        provisionRouter.get("/:id")
                .handler((ctx) -> this.get(ctx, Database.Table.MONITOR) );

        provisionRouter.post("/")
                .handler((ctx) -> this.insert(ctx, Database.Table.MONITOR) );

        provisionRouter.patch("/:id")
                .handler((ctx) -> this.update(ctx, Database.Table.MONITOR) );

        provisionRouter.delete("/:id")
                .handler((ctx) -> this.delete(ctx, Database.Table.MONITOR) );

        router.route(PROVISION_ENDPOINT).subRouter(provisionRouter);
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