package org.nms.HttpServer.Controllers;

import io.vertx.ext.web.RoutingContext;
import org.nms.App;
import org.nms.HttpServer.Utility.HttpResponse;

public class PollingController
{
    public static void getAllPolledData(RoutingContext ctx)
    {
        App.polledDataService
                .getAll()
                .onSuccess((polledData -> {
                    HttpResponse.sendSuccess(ctx, 200,"Polled Data", polledData);
                }))
                .onFailure(err -> {
                    HttpResponse.sendFailure(ctx, 500, "Something Went Wrong");
                });
    }
}
