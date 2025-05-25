package org.nms.database;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nms.App;
import org.nms.constants.Config;
import org.nms.constants.Fields;
import static org.nms.App.logger;

import java.util.*;

public class QueryBuilder {

    private JsonArray schema;

    private final String PRIMARY_KEYS = "primary_keys";

    private final String TABLE = "table";

    private final String COLUMNS = "columns";

    private final String COLUMN_NAME = "name";

    private final String COLUMN_TYPE = "type";

    private final String DEFAULT = "default";

    private static QueryBuilder instance;

    private QueryBuilder()
    {
        loadSchema();
    }

    public static QueryBuilder getInstance()
    {
        if(instance == null)
        {
            instance = new QueryBuilder();
        }

        return instance;
    }

    private void loadSchema()
    {
        App.vertx.fileSystem()
                .readFile(Config.SCHEMA_FILE_PATH)
                .onComplete(readResult ->
                {
                    if (readResult.succeeded())
                    {
                        this.schema = new JsonArray(readResult.result().toString());
                    }
                    else
                    {
                        logger.error("Failed to load schema: " + readResult.cause());
                    }
                });
    }

    public String build(String operation, String tableName, JsonObject params)
    {
        JsonObject tableSchema = findTableSchema(tableName);

        if (tableSchema.isEmpty())
        {
            logger.error("❌ Table not listed in schema.json");

            return "";
        }

        return switch (operation)
        {
            case Fields.Operations.CREATE_SCHEMA -> buildCreateSchemaQuery(tableName);

            case Fields.Operations.GET -> buildGetQuery(tableName);

            case Fields.Operations.LIST -> buildListQuery(tableName);

            case Fields.Operations.INSERT -> buildInsertQuery(tableName);

            case Fields.Operations.UPDATE -> buildUpdateQuery(tableName);

            case Fields.Operations.DELETE -> buildDeleteQuery(tableName);

            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }

    private String buildCreateSchemaQuery(String tableName)
    {
        var table = findTableSchema(tableName);

        var columns = table.getJsonArray(COLUMNS);

        var columnDefs = new ArrayList<String>();

        for (int j = 0; j < columns.size(); j++)
        {
            var col = columns.getJsonObject(j);

            var name = col.getString(COLUMN_NAME);

            var type = col.getString(COLUMN_TYPE);

            var def = col.getString(DEFAULT, "");

            String columnDef = def.isEmpty()
                    ? String.format("%s %s", name, type)
                    : String.format("%s %s DEFAULT %s", name, type, def);

            columnDefs.add(columnDef);
        }

        var pk = table.getJsonArray(PRIMARY_KEYS);

        if (!pk.isEmpty())
        {
            columnDefs.add(String.format("PRIMARY KEY (%s)", String.join(", ", pk.getList())));
        }

        return String.format("CREATE TABLE IF NOT EXISTS %s (%s);", tableName, String.join(", ", columnDefs));
    }

    private String buildGetQuery(String tableName)
    {
        var tableSchema = findTableSchema(tableName);

        var primaryKeys = tableSchema.getJsonArray(PRIMARY_KEYS);

        var conditions = new ArrayList<String>();

        for (var i = 0; i < primaryKeys.size(); i++)
        {
            var column = primaryKeys.getString(i);

            conditions.add(column + " = $" + (i + 1));
        }

        return String.format("SELECT * FROM %s WHERE %s;", tableName, String.join(" AND ", conditions));
    }

    private String buildListQuery(String tableName)
    {
        return String.format("SELECT * FROM %s;", tableName);
    }

    private String buildDeleteQuery(String tableName)
    {
        var tableSchema = findTableSchema(tableName);

        if (tableSchema.isEmpty())
        {
            logger.error("❌ Table not listed in schema.json");

            return "";
        }

        var primaryKeys = tableSchema.getJsonArray(PRIMARY_KEYS);

        var conditions = new ArrayList<String>();

        for (var i = 0; i < primaryKeys.size(); i++)
        {
            String column = primaryKeys.getString(i);

            conditions.add(column + " = $" + (i + 1));
        }

        return String.format("DELETE FROM %s WHERE %s;", tableName, String.join(" AND ", conditions));
    }

    private String buildInsertQuery(String tableName)
    {
        var tableSchema = findTableSchema(tableName);

        var columns = tableSchema.getJsonArray(COLUMNS);

        var columnNames = new ArrayList<String>();

        var placeholders = new ArrayList<String>();

        int paramIndex = 1;

        for (int i = 0; i < columns.size(); i++)
        {
            var column = columns.getJsonObject(i);

            var name = column.getString(COLUMN_NAME);

            var type = column.getString(COLUMN_TYPE);

            // Skip SERIAL (auto-increment) columns
            if (!"SERIAL".equalsIgnoreCase(type))
            {
                columnNames.add(name);

                placeholders.add("$" + (paramIndex++));
            }
        }

        return String.format(
                "INSERT INTO %s (%s) VALUES (%s) RETURNING *;",
                tableName,
                String.join(", ", columnNames),
                String.join(", ", placeholders)
        );
    }

    private String buildUpdateQuery(String tableName)
    {
        var tableSchema = findTableSchema(tableName);

        var columns = tableSchema.getJsonArray(COLUMNS);

        var primaryKeys = tableSchema.getJsonArray(PRIMARY_KEYS);

        var sets = new ArrayList<String>();

        var conditions = new ArrayList<String>();

        int paramIndex = 1;

        // SET clause for non-primary, non-serial columns
        for (int i = 0; i < columns.size(); i++)
        {
            JsonObject column = columns.getJsonObject(i);

            String name = column.getString(COLUMN_NAME);

            String type = column.getString(COLUMN_TYPE);

            if (!"SERIAL".equalsIgnoreCase(type) && !isPrimaryKey(name, primaryKeys))
            {
                sets.add(name + " = $" + (paramIndex++));
            }
        }

        // WHERE clause for primary keys, continue param indexing
        for (int i = 0; i < primaryKeys.size(); i++)
        {
            String key = primaryKeys.getString(i);

            String type = getColumnType(tableSchema, key);

            conditions.add(key + " = $" + (paramIndex++) + "::" + type);
        }

        if (sets.isEmpty())
        {
            throw new IllegalArgumentException("No columns to update (no non-primary key columns found).");
        }

        return String.format("UPDATE %s SET %s WHERE %s RETURNING *;",
                tableName,
                String.join(", ", sets),
                String.join(" AND ", conditions));
    }


    private boolean isPrimaryKey(String columnName, JsonArray primaryKeys)
    {
        for (var i = 0; i < primaryKeys.size(); i++)
        {
            if (columnName.equals(primaryKeys.getString(i)))
            {
                return true;
            }
        }

        return false;
    }

    private String getColumnType(JsonObject tableSchema, String columnName)
    {
        var columns = tableSchema.getJsonArray(COLUMN_TYPE);

        for (var i = 0; i < columns.size(); i++)
        {
            var col = columns.getJsonObject(i);

            if (col.getString(COLUMN_NAME).equals(columnName))
            {
                return col.getString(COLUMN_TYPE).toLowerCase();
            }
        }

        return "text"; // fallback
    }

    private JsonObject findTableSchema(String tableName)
    {
        for (int i = 0; i < schema.size(); i++)
        {
            JsonObject table = schema.getJsonObject(i);

            if (tableName.equals(table.getString(TABLE)))
            {
                return table;
            }
        }

        return new JsonObject();
    }
}
