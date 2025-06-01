package org.nms.database;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;
import org.nms.App;
import org.nms.constants.Database;
import org.nms.constants.Database.*;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Logger;

import static org.nms.App.LOGGER;
import static org.nms.constants.Database.Common.*;

public class QueryBuilder
{

    public static class SqlQuery
    {
        public String query;

        public Tuple parameters;

        public List<Tuple> batchParameters;

        public SqlQuery(String query, Tuple parameters)
        {
            this.query = query;

            this.parameters = parameters;
        }

        public SqlQuery(String query, List<Tuple> batchParameters)
        {
            this.query = query;

            this.batchParameters = batchParameters;
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

            Object value = condition.getValue(VALUE);

            conditionSql.add(column + " = $" + index++);

            values.addValue(value);
        }

        String sql = String.format("SELECT * FROM %s WHERE %s;", table, conditionSql);

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
            if(key.equals(ID)) continue;

            columns.add(key);

            placeholders.add("$" + index++);

            values.addValue(data.getValue(key));
        }

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s) RETURNING *;", table, columns, placeholders);

        return new SqlQuery(sql, values);
    }

    public static SqlQuery buildBatchInsert(String table, JsonArray data)
    {
        var firstObj = data.getJsonObject(0);

        var columns = new ArrayList<String>();

        for (var key : firstObj.fieldNames())
        {
            if (!key.equals(ID))
            {
                columns.add(key);
            }
        }

        var columnSql = String.join(", ", columns);

        var placeholderSql = new StringJoiner(", ", "(", ")");

        for (int i = 0; i < columns.size(); i++)
        {
            placeholderSql.add("$" + (i + 1));
        }

        var sql = String.format("INSERT INTO %s (%s) VALUES %s RETURNING *;", table, columnSql, placeholderSql);

        var batchParameters = new ArrayList<Tuple>();

        for (int i = 0; i < data.size(); i++)
        {
            var row = data.getJsonObject(i);

            var tuple = Tuple.tuple();

            for (var col : columns)
            {
                tuple.addValue(row.getValue(col));
            }

            batchParameters.add(tuple);
        }

        return new SqlQuery(sql, batchParameters);
    }


    public static SqlQuery buildUpdate(String table, JsonObject data, JsonArray conditions)
    {
        StringJoiner updates = new StringJoiner(", ");

        Tuple values = Tuple.tuple();

        int index = 1;

        for (String key : data.fieldNames())
        {
            if(key.equals(ID)) continue;

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

        String sql = String.format("UPDATE %s SET %s WHERE %s RETURNING *;", table, updates, conditionSql);

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

    public static SqlQuery buildSchema(String tableName)
    {
        var query = switch (tableName)
        {
            case(Table.USER_PROFILE) -> CreateSchemaQueries.USER_PROFILE;

            case(Table.CREDENTIAL_PROFILE) -> CreateSchemaQueries.CREDENTIAL_PROFILE;

            case(Table.DISCOVERY_PROFILE) -> CreateSchemaQueries.DISCOVERY_PROFILE;

            case(Table.DISCOVERY_CREDENTIAL) -> CreateSchemaQueries.DISCOVERY_CREDENTIAL;

            case(Table.DISCOVERY_RESULT) -> CreateSchemaQueries.DISCOVERY_RESULT;

            case(Table.MONITOR) -> CreateSchemaQueries.MONITOR;

            case(Table.METRIC) -> CreateSchemaQueries.METRIC;

            default -> CreateSchemaQueries.POLLING_RESULT;
        };

        return new SqlQuery(query, Tuple.tuple());
    }

    public static SqlQuery buildDefault()
    {
        return new SqlQuery("SELECT 'NMS API is connected to the database' AS status, NOW() AS current_time;", Tuple.tuple());
    }
}
