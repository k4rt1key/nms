package org.nms.HttpServer.Routers;

import io.vertx.ext.web.Router;
import org.nms.App;
import org.nms.HttpServer.Controllers.PollingController;

public class PollingRouter
{
    public static Router router()
    {
        var router = Router.router(App.vertx);

        router.get("/")
                .handler(PollingController::getAllPolledData);
        return router;
    }
}
