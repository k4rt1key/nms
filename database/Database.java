package org.nms.database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

import static org.nms.App.LOGGER;

import org.nms.constants.DatabaseConstant;
import org.nms.constants.EventbusAddress;

import java.util.ArrayList;

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
                startPromise.fail("❌ SqlClient is null");

                return;
            }

            vertx.eventBus().localConsumer(EventbusAddress.EXECUTE_QUERY, this::handleQuery);

            startPromise.complete();
        }
        catch (Exception exception)
        {
            startPromise.fail("❌ Failed to deploy Database Verticle");
        }
    }

    @Override
    public void stop(Promise<Void> stopPromise)
    {
        dbClient.close()
                .onComplete(result ->
                {
                    if (result.succeeded())
                    {
                        LOGGER.info("\uD83D\uDED1 Undeploy Database Verticle");

                        stopPromise.complete();
                    }
                    else
                    {
                        stopPromise.fail("❌ Failed to undeploy Database Verticle");
                    }
                });
    }

    private void handleQuery(Message<JsonObject> message)
    {
        JsonObject request = message.body();

        String query = request.getString(DatabaseConstant.QUERY);

        String mode = request.getString(DatabaseConstant.MODE);

        switch (mode)
        {
            case DatabaseConstant.SQL_QUERY_SINGLE_PARAM -> executeSingle(query, request.getJsonArray(DatabaseConstant.DATA), message);

            case DatabaseConstant.SQL_QUERY_BATCH -> executeBatch(query, request.getJsonArray(DatabaseConstant.BATCH_DATA), message);

            default -> executeWithoutParams(query, message);
        }
    }

    private void executeSingle(String query, JsonArray data, Message<JsonObject> message)
    {
        dbClient
                .preparedQuery(query)
                .execute(Tuple.wrap(data.getList().toArray()))
                .map(this::toJsonArray)
                .onComplete(result -> handleResult(result, message, query, data.encode()));
    }

    private void executeBatch(String query, JsonArray batchData, Message<JsonObject> message)
    {
        ArrayList<Tuple> tuples = new ArrayList<>();

        batchData.forEach(item ->
        {
            JsonArray row = (JsonArray) item;

            tuples.add(Tuple.from(row.getList().toArray()));
        });

        dbClient
                .preparedQuery(query)
                .executeBatch(tuples)
                .map(this::toJsonArray)
                .onComplete(result -> handleResult(result, message, query, batchData.encode()));
    }

    private void executeWithoutParams(String query, Message<JsonObject> message)
    {
        dbClient
                .preparedQuery(query)
                .execute()
                .map(this::toJsonArray)
                .onComplete(result -> handleResult(result, message, query, "[]"));
    }

    private void handleResult(io.vertx.core.AsyncResult<JsonArray> result, Message<JsonObject> message, String query, String input)
    {
        if (result.succeeded() && message.replyAddress() != null && !message.replyAddress().isEmpty())
        {
            message.reply(result.result());
        }
        else if (result.failed())
        {
            message.fail(500, String.format("Failed to execute => %s \nwith inputs => %s\nerror => %s", query, input, result.cause().getMessage()));
        }
    }

    private JsonArray toJsonArray(RowSet<Row> rows)
    {
        JsonArray results = new JsonArray();

        for (Row row : rows)
        {
            results.add(row.toJson());
        }

        return results;
    }
}
