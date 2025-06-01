package org.nms.api;

import io.vertx.ext.web.Router;

import org.nms.App;
import org.nms.constants.Database;
import org.nms.constants.Global;

public class Monitor implements AbstractHandler
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

        router.route(Global.MONITOR_ENDPOINT).subRouter(provisionRouter);
    }


}