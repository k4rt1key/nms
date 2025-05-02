package org.nms.HttpServer.Routers;

import io.vertx.ext.web.Router;
import org.nms.App;

public class PollingRouter
{
    public static Router router()
    {
        var router = Router.router(App.vertx);

        return router;
    }
}
