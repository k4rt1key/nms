package org.nms.utils;

import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Tuple;
import static org.nms.App.logger;
import static org.nms.App.vertx;

import org.nms.constants.Config;
import org.nms.constants.Fields;

import java.util.List;
import java.util.stream.Collectors;

public class ApiUtils
{
    public static Future<JsonArray> sendQueryExecutionRequest(String query, JsonArray params)
    {
        var request = new JsonObject()
                .put("query", query)
                .put("params", params);

        try
        {
            return vertx.eventBus().<JsonArray>request(
                            Fields.EventBus.EXECUTE_SQL_QUERY_WITH_PARAMS_ADDRESS,
                            request
                    )
                    .map(Message::body)
                    .onFailure(err ->
                            logger.warn("❌ Failed to execute...\n" + query + "\nwith params => " + params.encode() + "\nError => " + err.getMessage()));
        }

        catch (ReplyException replyException)
        {
            logger.warn("⚠ Query is taking more time then expected to execute");

            return Future.failedFuture("⚠ Query is taking more time then expected to execute");
        }

        catch (Exception exception)
        {
            return Future.failedFuture(exception);
        }
    }

    public static Future<JsonArray> sendQueryExecutionRequest(String query)
    {
        try
        {
            return vertx.eventBus().<JsonArray>request(
                            Fields.EventBus.EXECUTE_SQL_QUERY_ADDRESS,
                            query
                    )
                    .map(Message::body)
                    .onFailure(err -> logger.warn("❌ Failed to execute...\n" + query + "\nError => " + err.getMessage()));
        }

        catch (ReplyException replyException)
        {
            logger.warn("⚠ Query is taking more time then expected to execute");

            return Future.failedFuture("⚠ Query is taking more time then expected to execute");
        }

        catch (Exception exception)
        {
            return Future.failedFuture(exception);
        }
    }

    public static Future<JsonArray> sendQueryExecutionRequest(String query, List<Tuple> params)
    {
        // Convert List<Tuple> to JsonArray of JsonArrays for transmission over event bus
        var dbParams = new JsonArray();

        for (var tuple : params)
        {
            var paramArray = new JsonArray();

            // Convert each Tuple to JsonArray
            var size = tuple.size();

            for (var i = 0; i < size; i++)
            {
                paramArray.add(tuple.getValue(i));
            }

            dbParams.add(paramArray);
        }

        var request = new JsonObject()
                .put("query", query)
                .put("params", dbParams);

        try
        {
            return vertx.eventBus().<JsonArray>request(
                            Fields.EventBus.EXECUTE_SQL_QUERY_BATCH_ADDRESS,
                            request
                    )
                    .map(Message::body)
                    .onFailure(err ->
                            logger.warn("❌ Failed to execute...\n" + query + "\nWith params " + params.stream().map(Tuple::deepToString).collect(Collectors.joining(", ", "[", "]")) + "\nError => " + err.getMessage()));
        }

        catch (ReplyException replyException)
        {
            logger.warn("⚠ Query is taking more time then expected to execute");

            return Future.failedFuture("⚠ Query is taking more time then expected to execute");
        }

        catch (Exception exception)
        {
            return Future.failedFuture(exception);
        }
    }
}