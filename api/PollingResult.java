package org.nms.api;

import io.vertx.ext.web.Router;
import org.nms.constants.Database;

import static org.nms.App.VERTX;
import static org.nms.constants.Global.RESULT_ENDPOINT;

public class PollingResult implements AbstractHandler
{
    private static PollingResult instance;

    private PollingResult()
    {

    }

    public static PollingResult getInstance()
    {
        if(instance == null)
        {
            instance = new PollingResult();
        }

        return instance;
    }

    @Override
    public void init(Router router)
    {
        var pollResultRouter = Router.router(VERTX);

        pollResultRouter.get("/")
                .handler((ctx) -> this.list(ctx, Database.Table.POLLING_RESULT));

        router.route(RESULT_ENDPOINT).subRouter(pollResultRouter);
    }

}