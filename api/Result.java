package org.nms.api;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.nms.constants.Queries;
import org.nms.utils.DbUtils;

import static org.nms.constants.Fields.ENDPOINTS.RESULT_ENDPOINT;
import static org.nms.utils.ApiUtils.sendFailure;
import static org.nms.utils.ApiUtils.sendSuccess;

public class Result implements BaseHandler
{
    private static Result instance;

    private Result(){}

    public static Result getInstance()
    {
        if(instance == null)
        {
            instance =  new Result();
        }

        return instance;
    }

    @Override
    public void init(Router router)
    {
        router.route(RESULT_ENDPOINT).handler(this::list);
    }

    public void list(RoutingContext ctx)
    {
        DbUtils.sendQueryExecutionRequest(Queries.PollingResult.GET_ALL).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var polledData = asyncResult.result();

                sendSuccess(ctx, 200, "Polled Data", polledData);
            }
            else
            {
                sendFailure(ctx, 500, "Something Went Wrong");
            }
        });
    }

    @Override
    public void get(RoutingContext ctx)
    {
        sendFailure(ctx, 501, "Method not implemented");
    }

    @Override
    public void insert(RoutingContext ctx)
    {
        sendFailure(ctx, 501, "Method not implemented");
    }

    @Override
    public void update(RoutingContext ctx)
    {
        sendFailure(ctx, 501, "Method not implemented");
    }

    @Override
    public void delete(RoutingContext ctx)
    {
        sendFailure(ctx, 501, "Method not implemented");
    }
}