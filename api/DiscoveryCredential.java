package org.nms.api;

import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.nms.constants.Database;

import static org.nms.App.VERTX;
import static org.nms.constants.Fields.ENDPOINTS.DISCOVERY_CREDENTIAL_ENDPOINT;

public class DiscoveryCredential implements BaseHandler
{
    private static DiscoveryCredential instance;

    private DiscoveryCredential()
    {

    }

    public static DiscoveryCredential getInstance()
    {
        if(instance == null)
        {
            instance = new DiscoveryCredential();
        }

        return instance;
    }

    @Override
    public void init(Router router)
    {
        var discoveryCredentialRouter = Router.router(VERTX);

        discoveryCredentialRouter.get("/")
                .handler((ctx) -> this.list(ctx, Database.Table.DISCOVERY_CREDENTIAL));

        discoveryCredentialRouter.get("/:id")
                .handler((ctx) -> this.get(ctx, Database.Table.DISCOVERY_CREDENTIAL));

        discoveryCredentialRouter.post("/")
                .handler((ctx) -> this.insert(ctx, Database.Table.DISCOVERY_CREDENTIAL));

        discoveryCredentialRouter.patch("/:id")
                .handler((ctx) -> this.update(ctx, Database.Table.DISCOVERY_CREDENTIAL));

        discoveryCredentialRouter.delete("/:id")
                .handler((ctx) -> this.delete(ctx, Database.Table.DISCOVERY_CREDENTIAL));

        router.route(DISCOVERY_CREDENTIAL_ENDPOINT).subRouter(discoveryCredentialRouter);
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