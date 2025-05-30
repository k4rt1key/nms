package org.nms.database;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;
import org.nms.App;
import org.nms.constants.Database.*;

import java.util.StringJoiner;

import static org.nms.constants.Database.Common.COLUMN;
import static org.nms.constants.Database.Common.VALUE;

public class QueryBuilder
{

    public static class SqlQuery
    {
        public final String query;

        public final Tuple parameters;

        public SqlQuery(String query, Tuple parameters)
        {
            this.query = query;

            this.parameters = parameters;
        }
    }

    public static SqlQuery buildSelectAll(String table)
    {
        return new SqlQuery(String.format("SELECT * FROM %s;", table), Tuple.tuple());
    }

    public static SqlQuery buildSelectByCondition(String table, JsonArray conditions)
    {
        Tuple values = Tuple.tuple();

        StringJoiner conditionSql = new StringJoiner(" AND ");

        int index = 1;

        for (int i = 0; i < conditions.size(); i++)
        {
            JsonObject condition = conditions.getJsonObject(i);

            String column = condition.getString(COLUMN);

            Object value = condition.getValue(Common.VALUE);

            conditionSql.add(column + " = $" + index++);

            values.addValue(value);
        }

        String sql = String.format("SELECT * FROM %s WHERE %s;", table, conditionSql);

        App.LOGGER.debug(sql + values.getValue(0));

        return new SqlQuery(sql, values);
    }

    public static SqlQuery buildInsert(String table, JsonObject data)
    {
        StringJoiner columns = new StringJoiner(", ");

        StringJoiner placeholders = new StringJoiner(", ");

        Tuple values = Tuple.tuple();

        int index = 1;

        for (String key : data.fieldNames())
        {
            columns.add(key);

            placeholders.add("$" + index++);

            values.addValue(data.getValue(key));
        }

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s) RETURNING id;", table, columns, placeholders);

        return new SqlQuery(sql, values);
    }

    public static SqlQuery buildUpdate(String table, JsonObject data, JsonArray conditions)
    {
        StringJoiner updates = new StringJoiner(", ");

        Tuple values = Tuple.tuple();

        int index = 1;

        for (String key : data.fieldNames())
        {
            updates.add(key + " = COALESCE($" + index++ + ", " + key + ")");

            values.addValue(data.getValue(key));
        }

        StringJoiner conditionSql = new StringJoiner(" AND ");

        for (int i = 0; i < conditions.size(); i++)
        {
            JsonObject condition = conditions.getJsonObject(i);

            String column = condition.getString(COLUMN);

            Object value = condition.getValue(VALUE);

            conditionSql.add("(" + column + " = COALESCE($" + index++ + ", " + column + "))");

            values.addValue(value);
        }

        String sql = String.format("UPDATE %s SET %s WHERE %s RETURNING id;", table, updates, conditionSql);

        return new SqlQuery(sql, values);
    }

    public static SqlQuery buildDelete(String table, JsonArray conditions)
    {
        Tuple values = Tuple.tuple();

        StringJoiner conditionSql = new StringJoiner(" AND ");

        int index = 1;

        for (int i = 0; i < conditions.size(); i++)
        {
            JsonObject condition = conditions.getJsonObject(i);

            String column = condition.getString(COLUMN);

            Object value = condition.getValue(VALUE);

            conditionSql.add(column + " = $" + index++);

            values.addValue(value);
        }

        String sql = String.format("DELETE FROM %s WHERE %s RETURNING *;", table, conditionSql);

        return new SqlQuery(sql, values);
    }

    public static SqlQuery buildSchema()
    {
        return new SqlQuery(Common.CREATE_ALL_SCHEMAS, Tuple.tuple());
    }

    public static SqlQuery buildDefault()
    {
        return new SqlQuery("SELECT 'NMS API is connected to the database' AS status, NOW() AS current_time;", Tuple.tuple());
    }
}
