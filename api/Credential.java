package org.nms.api;

import io.vertx.ext.web.Router;
import org.nms.constants.Database;
import org.nms.constants.Global;

import static org.nms.App.VERTX;

public class Credential implements AbstractHandler
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
                .handler((ctx) -> this.list(ctx, Database.Table.CREDENTIAL_PROFILE));

        credentialRouter.get("/:id")
                .handler((ctx) -> this.get(ctx, Database.Table.CREDENTIAL_PROFILE));

        credentialRouter.post("/")
                .handler((ctx) -> this.insert(ctx, Database.Table.CREDENTIAL_PROFILE));

        credentialRouter.patch("/:id")
                .handler((ctx) -> this.update(ctx, Database.Table.CREDENTIAL_PROFILE));

        credentialRouter.delete("/:id")
                .handler((ctx) -> this.delete(ctx, Database.Table.CREDENTIAL_PROFILE));

        router.route(Global.CREDENTIALS_ENDPOINT).subRouter(credentialRouter);
    }

}