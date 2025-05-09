package org.nms.database;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.nms.ConsoleLogger;

import java.util.List;

public class DbEngine
{

    public static Future<JsonArray> execute(String sql, JsonArray params)
    {
        if(DbClient.client != null)
        {
            return DbClient
                    .client
                    .preparedQuery(sql)
                    .execute(Tuple.wrap(params.getList().toArray()))
                    .map(DbEngine::toJsonArray)
                    .onSuccess(result -> ConsoleLogger.info("✅ Successfully executed \n\uD83D\uDE80 " + sql + "\n\uD83D\uDE80 With Params" + params.encode()))
                    .onFailure(err -> ConsoleLogger.error("❌ Failed to execute " + sql + ", error => " + err.getMessage()));
        }
        else
        {
            ConsoleLogger.error("❌ Postgres Client Is Null");

            return Future.failedFuture("❌ Postgres Client Is Null");
        }
    }

    public static Future<JsonArray> execute(String sql)
    {
        if(DbClient.client != null)
        {
            return DbClient
                    .client
                    .preparedQuery(sql)
                    .execute()
                    .map(DbEngine::toJsonArray);
        }
        else
        {
            ConsoleLogger.error("❌ Postgres Client Is Null");

            return Future.failedFuture("❌ Postgres Client Is Null");
        }
    }

    public static Future<JsonArray> execute(String sql, List<Tuple> params)
    {
        if(DbClient.client != null)
        {
            return DbClient
                    .client
                    .preparedQuery(sql)
                    .executeBatch(params)
                    .map(DbEngine::toJsonArray)
                    .onSuccess(result -> ConsoleLogger.info("✅ Successfully executed \n\uD83D\uDE80 " + sql + "\n\uD83D\uDE80 With Params " + params.stream().toArray().toString()))
                    .onFailure(err -> ConsoleLogger.error("❌ Failed to execute " + sql + ", error => " + err.getMessage()));
        }
        else
        {
            ConsoleLogger.error("❌ Postgres Client Is Null");

            return Future.failedFuture("❌ Postgres Client Is Null");
        }

    }

    public static JsonArray toJsonArray(RowSet<Row> rows)
    {
        var jsonArray = new JsonArray();

        for (Row row : rows)
        {
            jsonArray.add(row.toJson());
        }

        return jsonArray;
    }
}
