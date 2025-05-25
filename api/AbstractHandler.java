package org.nms.api;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.nms.constants.Fields;
import org.nms.constants.Queries;
import org.nms.database.QueryBuilder;
import org.nms.utils.DbUtils;

import static org.nms.utils.ApiUtils.sendFailure;
import static org.nms.utils.ApiUtils.sendSuccess;

public interface AbstractHandler
{
    void init(Router router);

    static void list(RoutingContext ctx, String tableName)
    {
        DbUtils.sendQueryExecutionRequest(QueryBuilder.getInstance().build(Fields.Operations.LIST, tableName, null))
                .onComplete(asyncResult ->
                {
                    if (asyncResult.succeeded())
                    {
                        var data = asyncResult.result();

                        if (data.isEmpty())
                        {
                            sendFailure(ctx, 404, "No " + tableName + " found");

                            return;
                        }

                        sendSuccess(ctx, 200, tableName + " found", data);
                    }
                    else
                    {
                        sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
                    }
                });
    };

    static void get(RoutingContext ctx, String tableName)
    {
        DbUtils.sendQueryExecutionRequest(QueryBuilder.getInstance().build(Fields.Operations.GET, tableName, null))
                .onComplete(asyncResult ->
                {
                    if (asyncResult.succeeded())
                    {
                        var data = asyncResult.result();

                        if (data.isEmpty())
                        {
                            sendFailure(ctx, 404, "No " + tableName + " found");

                            return;
                        }

                        sendSuccess(ctx, 200, tableName + " found", data);
                    }
                    else
                    {
                        sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
                    }
                });
    };

    void insert(RoutingContext ctx);

    void update(RoutingContext ctx);

    void delete(RoutingContext ctx);
}
