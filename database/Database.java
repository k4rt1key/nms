package org.nms.database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;

import static org.nms.App.LOGGER;
import static org.nms.constants.Eventbus.EXECUTE_QUERY;

import org.nms.constants.Database.*;

public class Database extends AbstractVerticle
{
    private SqlClient dbClient;

    @Override
    public void start(Promise<Void> startPromise)
    {
        try
        {
            dbClient = PgClient.getInstance().getSqlClient();

            if (dbClient == null)
            {
                startPromise.fail("SqlClient is null");
            }

            vertx.eventBus().localConsumer(EXECUTE_QUERY, this::execute);

            LOGGER.info("Successfully deployed Database Verticle");

            startPromise.complete();
        }
        catch (Exception exception)
        {
            startPromise.fail("❌ Failed to deploy database, error => " + exception.getMessage());
        }
    }

    @Override
    public void stop(Promise<Void> stopPromise)
    {
        dbClient
                .close()

                .onComplete(asyncResult ->
                {
                    if(asyncResult.succeeded())
                    {
                        LOGGER.info("Database Verticle Stopped");

                        stopPromise.complete();
                    }
                    else
                    {
                        stopPromise.fail("Failed to close database connection, error => " + asyncResult.cause().getMessage());
                    }
                });
    }

    private void execute(Message<JsonObject> message)
    {
        var operation = message.body().getString(Common.OPERATION);

        var tableName = message.body().getString(Common.TABLE_NAME);

        var conditions = message.body().getJsonArray(Common.CONDITION);

        var data = message.body().getJsonObject(Common.DATA);

        QueryBuilder.SqlQuery sqlQuery = switch (operation)
        {
            case Operation.LIST -> QueryBuilder.buildSelectAll(tableName);

            case Operation.GET -> QueryBuilder.buildSelectByCondition(tableName, conditions);

            case Operation.INSERT -> QueryBuilder.buildInsert(tableName, data);

            case Operation.UPDATE -> QueryBuilder.buildUpdate(tableName, data, conditions);

            case Operation.DELETE -> QueryBuilder.buildDelete(tableName, conditions);

            default -> QueryBuilder.buildDefault();
        };

        dbClient.preparedQuery(sqlQuery.query)

                .execute(sqlQuery.parameters)

                .map(this::toJsonArray)

                .onComplete(dbResult ->
                {
                    if(dbResult.succeeded())
                    {
                        message.reply(dbResult.result());
                    }
                    else
                    {
                        message.fail(500, "Failed to execute " + operation + " operation on table " + tableName + " error => " + dbResult.cause().getMessage());
                    }
                });
    }

    private JsonArray toJsonArray(RowSet<Row> rows)
    {
        var results = new JsonArray();

        for (Row row : rows)
        {
            results.add(row.toJson());
        }

        return results;
    }
}
