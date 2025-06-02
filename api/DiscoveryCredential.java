package org.nms.api;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import org.nms.constants.Database;
import org.nms.constants.Eventbus;
import org.nms.constants.Global;
import org.nms.utils.ApiUtils;
import org.nms.utils.DbUtils;
import org.nms.validators.Validators;

import static org.nms.App.VERTX;

public class DiscoveryCredential implements AbstractHandler
{
    private static DiscoveryCredential instance;

    private DiscoveryCredential()
    {

    }

    public static DiscoveryCredential getInstance()
    {
        if (instance == null)
        {
            instance = new DiscoveryCredential();
        }

        return instance;
    }

    @Override
    public void init(Router router)
    {
        var discoveryCredentialRouter = Router.router(VERTX);

        discoveryCredentialRouter.get("/:discovery_profile")
                .handler(ctx -> this.get(ctx, Database.Table.DISCOVERY_CREDENTIAL));

        discoveryCredentialRouter.delete("/:discovery_profile/:credential_profile")
                .handler(ctx -> this.delete(ctx, Database.Table.DISCOVERY_CREDENTIAL));

        discoveryCredentialRouter.post("/")
                .handler(ctx -> this.insert(ctx, Database.Table.DISCOVERY_CREDENTIAL));

        router.route(Global.DISCOVERY_CREDENTIAL_ENDPOINT).subRouter(discoveryCredentialRouter);
    }

}
