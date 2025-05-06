package org.nms.Database;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.List;

public class PostgresQuery
{
    public static Future<RowSet<Row>> execute(String sql, JsonArray params)
    {
        return PostgresClient.client.preparedQuery(sql).execute(Tuple.wrap(params.getList().toArray()));
    }

    public static Future<RowSet<Row>> execute(String sql)
    {
        return PostgresClient.client.preparedQuery(sql).execute();
    }

    public static Future<RowSet<Row>> execute(String sql, List<Tuple> params)
    {
        return PostgresClient.client.preparedQuery(sql).executeBatch(params);
    }

    public static JsonArray toJsonArray(RowSet<Row> rows)
    {
        JsonArray jsonArray = new JsonArray();
        for (Row row : rows) {
            jsonArray.add(row.toJson());
        }
        return jsonArray;
    }
}
