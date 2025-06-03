package org.nms.api;

import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.nms.App;
import org.nms.constants.DatabaseConstant;
import org.nms.constants.EventbusAddress;
import org.nms.utils.ApiUtils;
import org.nms.utils.DbUtils;
import org.nms.validators.Validators;


public interface AbstractHandler
{
    void init(Router router);

    default void get(RoutingContext ctx, String query)
    {
        var id = Validators.validateID(ctx);

        if(id == -1) return;

        var queryRequest = DbUtils.buildRequest(
                query,
                new JsonArray().add(id),
                DatabaseConstant.SQL_QUERY_SINGLE_PARAM
        );

        App.VERTX.eventBus().<JsonArray>request(EventbusAddress.EXECUTE_QUERY, queryRequest, dbResponse ->
        {
            if(dbResponse.succeeded())
            {
                if(dbResponse.result().body() == null || dbResponse.result().body().isEmpty())
                {
                    ApiUtils.sendFailure(ctx, 404, "No Data Found");
                }
                else
                {
                    ApiUtils.sendSuccess(ctx, 200, dbResponse.result().body());

                    deleteCache(dbResponse.result().body());
                }
            }
            else
            {
                ApiUtils.sendFailure(ctx, 500, "Something Went Wrong", dbResponse.cause().getMessage());
            }
        });

    }

    default void list(RoutingContext ctx, String query)
    {
        var id = Validators.validateID(ctx);

        if(id == -1) return;

        var queryRequest = DbUtils.buildRequest(
                query,
                null,
                DatabaseConstant.SQL_QUERY_WITHOUT_PARAMS
        );

        App.VERTX.eventBus().<JsonArray>request(EventbusAddress.EXECUTE_QUERY, queryRequest, dbResponse ->
        {
            if(dbResponse.succeeded())
            {
                if(dbResponse.result().body() == null || dbResponse.result().body().isEmpty())
                {
                    ApiUtils.sendFailure(ctx, 404, "No Data Found");
                }
                else
                {
                    ApiUtils.sendSuccess(ctx, 200, dbResponse.result().body());

                    deleteCache(dbResponse.result().body());
                }
            }
            else
            {
                ApiUtils.sendFailure(ctx, 500, "Something Went Wrong", dbResponse.cause().getMessage());
            }
        });

    }

    void insert(RoutingContext ctx);

    void update(RoutingContext ctx);

    default void delete(RoutingContext ctx, String query)
    {
        var id = Validators.validateID(ctx);

        if(id == -1) return;

        var queryRequest = DbUtils.buildRequest(
                query,
                new JsonArray().add(id),
                DatabaseConstant.SQL_QUERY_SINGLE_PARAM
        );

        App.VERTX.eventBus().<JsonArray>request(EventbusAddress.EXECUTE_QUERY, queryRequest, dbResponse ->
        {
            if(dbResponse.succeeded())
            {
                if(dbResponse.result().body() == null || dbResponse.result().body().isEmpty())
                {
                    ApiUtils.sendFailure(ctx, 404, "No Data Found");
                }
                else
                {
                    ApiUtils.sendSuccess(ctx, 200, dbResponse.result().body());

                    deleteCache(dbResponse.result().body());
                }
            }
            else
            {
                ApiUtils.sendFailure(ctx, 500, "Something Went Wrong", dbResponse.cause().getMessage());
            }
        });

    }

    void updateCache(JsonArray data);

    void insertCache(JsonArray data);

    void deleteCache(JsonArray data);

}