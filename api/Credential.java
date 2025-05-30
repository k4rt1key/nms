package org.nms.api;

import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.nms.constants.Database;

import static org.nms.App.VERTX;
import static org.nms.constants.Fields.ENDPOINTS.CREDENTIALS_ENDPOINT;

public class Credential implements BaseHandler
{
    private static Credential instance;

    private Credential()
    {

    }

    public static Credential getInstance()
    {
        if(instance == null)
        {
            instance = new Credential();
        }

        return instance;
    }

    @Override
    public void init(Router router)
    {
        var credentialRouter = Router.router(VERTX);

        credentialRouter.get("/")
                .handler((ctx) -> this.list(ctx, Database.Table.CREDENTIAL));

        credentialRouter.get("/:id")
                .handler((ctx) -> this.get(ctx, Database.Table.CREDENTIAL));

        credentialRouter.post("/")
                .handler((ctx) -> this.insert(ctx, Database.Table.CREDENTIAL));

        credentialRouter.patch("/:id")
                .handler((ctx) -> this.update(ctx, Database.Table.CREDENTIAL));

        credentialRouter.delete("/:id")
                .handler((ctx) -> this.delete(ctx, Database.Table.CREDENTIAL));

        router.route(CREDENTIALS_ENDPOINT).subRouter(credentialRouter);
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