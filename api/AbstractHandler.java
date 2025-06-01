package org.nms.api;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.nms.App;
import org.nms.constants.Database;
import org.nms.utils.ApiUtils;
import org.nms.utils.DbUtils;

import static org.nms.constants.Database.Common.*;
import static org.nms.constants.Eventbus.*;

public interface AbstractHandler
{
    void init(Router router);

    default void list(RoutingContext ctx, String tableName)
    {
        var queryRequest = DbUtils.buildRequest (
                tableName,
                Database.Operation.LIST,
                null,
                null,
                null
        );

        App.VERTX.eventBus().<JsonArray>request(EXECUTE_QUERY, queryRequest, dbResponse ->
        {
            if(dbResponse.succeeded())
            {
                if(dbResponse.result().body() == null || dbResponse.result().body().isEmpty())
                {
                    ApiUtils.sendSuccess(ctx, 200, "No Data Found", new JsonArray());
                }
                else
                {
                    ApiUtils.sendSuccess(ctx, 200, "Successfully LIST " + tableName, dbResponse.result().body());
                }
            }
            else
            {
                ApiUtils.sendFailure(ctx, 500, "Something Went Wrong", "Failed to LIST " + tableName + " : " + dbResponse.cause().getMessage());
            }
        });
    }

    default void get(RoutingContext ctx, String tableName)
    {
        var params = ctx.pathParams();

        var conditions = new JsonArray();

        for(String key : params.keySet())
        {
            try
            {
                Integer.parseInt(params.get(key));

                conditions.add(new JsonObject().put(COLUMN, key).put(VALUE, Integer.parseInt(params.get(key))));
            }
            catch (Exception exception)
            {
                conditions.add(new JsonObject().put(COLUMN, key).put(VALUE, params.get(key)));
            }
        }


        var queryRequest = DbUtils.buildRequest(
                tableName,
                Database.Operation.GET,
                conditions,
                null,
                null
        );

        App.VERTX.eventBus().<JsonArray>request(EXECUTE_QUERY, queryRequest, dbResponse ->
        {
            if(dbResponse.succeeded())
            {
                if(dbResponse.result().body() == null || dbResponse.result().body().isEmpty())
                {
                    ApiUtils.sendSuccess(ctx, 200, "No Data Found", new JsonArray());
                }
                else
                {
                    ApiUtils.sendSuccess(ctx, 200, "Successfully GET " + tableName, dbResponse.result().body());
                }
            }
            else
            {
                ApiUtils.sendFailure(ctx, 500, "Something Went Wrong", "Failed to GET " + tableName + " : " + dbResponse.cause().getMessage());
            }
        });
    }

    default void insert(RoutingContext ctx, String tableName)
    {
        var queryRequest = DbUtils.buildRequest(
                tableName,
                Database.Operation.INSERT,
                null,
                ctx.body().asJsonObject(),
                null
        );

        App.VERTX.eventBus().<JsonArray>request(EXECUTE_QUERY, queryRequest, dbResponse ->
        {
            if(dbResponse.succeeded() && dbResponse.result().body() != null && !dbResponse.result().body().isEmpty())
            {
                    ApiUtils.sendSuccess(ctx, 200, "Successfully INSERT INTO " + tableName, dbResponse.result().body());
            }
            else
            {
                    ApiUtils.sendFailure(ctx, 500, "Something Went Wrong", "Failed to INSERT " + tableName + " : " + dbResponse.cause().getMessage());
            }
        });

    }

    default void update(RoutingContext ctx, String tableName)
    {
        var params = ctx.pathParams();

        var conditions = new JsonArray();

        for(String key : params.keySet())
        {
            try
            {
                Integer.parseInt(params.get(key));

                conditions.add(new JsonObject().put(COLUMN, key).put(VALUE, Integer.parseInt(params.get(key))));
            }
            catch (Exception exception)
            {
                conditions.add(new JsonObject().put(COLUMN, key).put(VALUE, params.get(key)));
            }
        }

        var queryRequest = DbUtils.buildRequest(
                tableName,
                Database.Operation.UPDATE,
                conditions,
                ctx.body().asJsonObject(),
                null
        );

        App.VERTX.eventBus().<JsonArray>request(EXECUTE_QUERY, queryRequest, dbResponse ->
        {
            if(dbResponse.succeeded() && dbResponse.result().body() != null && !dbResponse.result().body().isEmpty())
            {
                ApiUtils.sendSuccess(ctx, 200, "Successfully UPDATE " + tableName, dbResponse.result().body());
            }
            else
            {
                ApiUtils.sendFailure(ctx, 500, "Something Went Wrong", "Failed to UPDATE " + tableName + " : " + dbResponse.cause().getMessage());
            }
        });

    }

    default void delete(RoutingContext ctx, String tableName)
    {
        var params = ctx.pathParams();

        var conditions = new JsonArray();

        for(String key : params.keySet())
        {
            try
            {
                Integer.parseInt(params.get(key));

                conditions.add(new JsonObject().put(COLUMN, key).put(VALUE, Integer.parseInt(params.get(key))));
            }
            catch (Exception exception)
            {
                conditions.add(new JsonObject().put(COLUMN, key).put(VALUE, params.get(key)));
            }
        }

        var queryRequest = DbUtils.buildRequest(
                tableName,
                Database.Operation.DELETE,
                conditions,
                null,
                null
        );

        App.VERTX.eventBus().<JsonArray>request(EXECUTE_QUERY, queryRequest, dbResponse ->
        {
            if(dbResponse.succeeded())
            {
                if(dbResponse.result().body() == null || dbResponse.result().body().isEmpty())
                {
                    ApiUtils.sendFailure(ctx, 404, "No Data Found");
                }
                else
                {
                    ApiUtils.sendSuccess(ctx, 200, "Successfully DELETE " + tableName, dbResponse.result().body());

                    // TODO : Delete in cache
                }
            }
            else
            {
                ApiUtils.sendFailure(ctx, 500, "Something Went Wrong", "Failed to DELETE " + tableName + " : " + dbResponse.cause().getMessage());
            }
        });

    }

}
