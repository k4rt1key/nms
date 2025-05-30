package org.nms.api;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.nms.constants.Database;

import static org.nms.App.VERTX;
import static org.nms.constants.Fields.ENDPOINTS.DISCOVERY_CREDENTIAL_ENDPOINT;

public class Metric implements BaseHandler
{
    private static Metric instance;

    private Metric()
    {

    }

    public static Metric getInstance()
    {
        if(instance == null)
        {
            instance = new Metric();
        }

        return instance;
    }

    @Override
    public void init(Router router)
    {
        var metricRouter = Router.router(VERTX);

        metricRouter.get("/")
                .handler((ctx) -> this.list(ctx, Database.Table.METRIC));

        metricRouter.get("/:id")
                .handler((ctx) -> this.get(ctx, Database.Table.METRIC));

        metricRouter.post("/")
                .handler((ctx) -> this.insert(ctx, Database.Table.METRIC));

        metricRouter.patch("/:id")
                .handler((ctx) -> this.update(ctx, Database.Table.METRIC));

        metricRouter.delete("/:id")
                .handler((ctx) -> this.delete(ctx, Database.Table.METRIC));

        router.route(DISCOVERY_CREDENTIAL_ENDPOINT).subRouter(metricRouter);
    }

    @Override
    public void beforeInsert(RoutingContext ctx)
    {

    }

    @Override
    public void afterInsert(RoutingContext ctx)
    {

    }

    @Override
    public void beforeUpdate(RoutingContext ctx)
    {

    }

    @Override
    public void afterUpdate(RoutingContext ctx)
    {

    }

}