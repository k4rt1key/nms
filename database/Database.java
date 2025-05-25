package org.nms.database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;
import static org.nms.App.logger;

import org.nms.App;
import org.nms.constants.Config;
import org.nms.constants.Fields;
import org.nms.constants.Queries;
import org.nms.utils.DbUtils;

import java.util.ArrayList;
import java.util.List;

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

            // Single unified handler for all query execution types
            vertx.eventBus().localConsumer(Fields.EventBus.EXECUTE_SQL_QUERY_ADDRESS, this::handleExecuteQuery);
            vertx.eventBus().localConsumer(Fields.EventBus.EXECUTE_SQL_QUERY_WITH_PARAMS_ADDRESS, this::handleExecuteQuery);
            vertx.eventBus().localConsumer(Fields.EventBus.EXECUTE_SQL_QUERY_BATCH_ADDRESS, this::handleExecuteQuery);

            Future.join(List.of(
                    DbUtils.sendQueryExecutionRequest(Queries.User.CREATE_SCHEMA),
                    DbUtils.sendQueryExecutionRequest(Queries.Credential.CREATE_SCHEMA),
                    DbUtils.sendQueryExecutionRequest(Queries.Discovery.CREATE_SCHEMA),
                    DbUtils.sendQueryExecutionRequest(Queries.Discovery.CREATE_DISCOVERY_CREDENTIAL_SCHEMA),
                    DbUtils.sendQueryExecutionRequest(Queries.Discovery.CREATE_DISCOVERY_RESULT_SCHEMA),
                    DbUtils.sendQueryExecutionRequest(Queries.Monitor.CREATE_SCHEMA),
                    DbUtils.sendQueryExecutionRequest(Queries.Monitor.CREATE_METRIC_GROUP_SCHEMA),
                    DbUtils.sendQueryExecutionRequest(Queries.PollingResult.CREATE_SCHEMA)
            )).onComplete(allSchemasCreated ->
            {
                if(allSchemasCreated.succeeded())
                {
                    logger.info("✅ Successfully deployed Database Verticle");
                    startPromise.complete();
                }
                else
                {
                    logger.warn("⚠ Something went wrong creating db schema");
                    startPromise.fail(allSchemasCreated.cause());
                }
            });
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
                .onComplete(clientClose ->
                {
                    if(clientClose.succeeded())
                    {
                        logger.info("\uD83D\uDED1 Database Verticle Stopped");
                        stopPromise.complete();
                    }
                    else
                    {
                        stopPromise.fail("❌ Failed to close database connection, error => " + clientClose.cause().getMessage());
                    }
                });
    }

    /**
     * Unified handler that automatically detects the message type and executes the appropriate query method
     */
    private void handleExecuteQuery(Message<?> message)
    {
        try
        {
            Object messageBody = message.body();

            // Simple query without parameters (String message)
            if (messageBody instanceof String)
            {
                String query = (String) messageBody;

                dbClient.preparedQuery(query)
                        .execute()
                        .map(this::toJsonArray)
                        .onComplete(dbResult ->
                        {
                            if(dbResult.succeeded())
                            {
                                message.reply(dbResult.result());
                            }
                            else
                            {
                                message.fail(500, "❌ Failed to execute simple query...\n" +
                                        query + "\nError => " + dbResult.cause().getMessage());
                            }
                        });
            }

            // Query with parameters or batch query (JsonObject message)
            else if (messageBody instanceof JsonObject)
            {
                JsonObject request = (JsonObject) messageBody;
                String query = request.getString("query");
                JsonArray paramsArray = request.getJsonArray("params");

                if (paramsArray == null || paramsArray.isEmpty())
                {
                    // Handle as simple query if no params
                    dbClient.preparedQuery(query)
                            .execute()
                            .map(this::toJsonArray)
                            .onComplete(dbResult ->
                            {
                                if(dbResult.succeeded())
                                {
                                    message.reply(dbResult.result());
                                }
                                else
                                {
                                    message.fail(500, "❌ Failed to execute query without params...\n" +
                                            query + "\nError => " + dbResult.cause().getMessage());
                                }
                            });
                }
                // Check if it's a batch query (array of arrays) or single parameter query
                else if (paramsArray.size() > 0 && paramsArray.getValue(0) instanceof JsonArray)
                {
                    // Batch query execution
                    var tuples = new ArrayList<Tuple>();

                    for (int i = 0; i < paramsArray.size(); i++)
                    {
                        var params = paramsArray.getJsonArray(i);
                        tuples.add(Tuple.wrap(params.getList().toArray()));
                    }

                    dbClient.preparedQuery(query)
                            .executeBatch(tuples)
                            .map(this::toJsonArray)
                            .onComplete(dbResult ->
                            {
                                if(dbResult.succeeded())
                                {
                                    message.reply(dbResult.result());
                                }
                                else
                                {
                                    message.fail(500, "❌ Failed to execute batch query...\n" +
                                            query + "\nError => " + dbResult.cause().getMessage());
                                }
                            });
                }
                else
                {
                    // Single parameter query execution
                    dbClient.preparedQuery(query)
                            .execute(Tuple.wrap(paramsArray.getList().toArray()))
                            .map(this::toJsonArray)
                            .onComplete(dbResult ->
                            {
                                if(dbResult.succeeded())
                                {
                                    message.reply(dbResult.result());
                                }
                                else
                                {
                                    message.fail(500, "❌ Failed to execute parameterized query...\n" +
                                            query + "\nError => " + dbResult.cause().getMessage());
                                }
                            });
                }
            }
            else
            {
                // Unsupported message type
                String messageType = messageBody != null ? messageBody.getClass().getSimpleName() : "null";
                String errorMsg = "❌ Unsupported message type: " + messageType +
                        ". Supported types: String or JsonObject";
                logger.warn(errorMsg);
                message.fail(400, errorMsg);
            }
        }
        catch (Exception exception)
        {
            logger.error("❌ Unexpected error during query execution", exception);
            message.fail(500, "❌ Unexpected error: " + exception.getMessage());
        }
    }

    private JsonArray toJsonArray(RowSet<Row> rows)
    {
        var results = new JsonArray();

        for (Row row : rows) {
            results.add(row.toJson());
        }

        return results;
    }

    private JsonArray loadSchema(String path)
    {
        App.vertx.fileSystem()
                .readFile(Config.SCHEMA_FILE_PATH)
                .onComplete(readResult ->
                {

                });
    }

    public String buildQuery(String operation, String dbName, JsonArray params)
    {
        return switch (operation)
        {
            case (Fields.Operations.GET) -> buildGetQuery(dbName);
            case (Fields.Operations.LIST) -> buildListQuery(dbName);
            case (Fields.Operations.INSERT) -> buildInsertQuery(dbName, params);
            case (Fields.Operations.UPDATE) -> buildUpdateQuery(dbName, params);
            case (Fields.Operations.DELETE) -> buildDeleteQuery(dbName);
            default -> "";
        };
    }

    private String buildGetQuery(String dbName)
    {
        return String.format("SELECT * FROM %s WHERE ID = $;", dbName);
    }

    private String buildListQuery(String dbName)

    {
        return String.format("SELECT * FROM %s;", dbName);
    }

    private String buildDeleteQuery(String dbName)
    {
        return String.format("DELETE FROM %s WHERE ID = $;", dbName);
    }

    private String buildInsertQuery(String dbName, JsonArray params)
    {
        return String.format("INSERT INTO %s VALUES(%s) RETURNING id;", dbName, params);
    }

    private String buildUpdateQuery(String dbName, JsonArray params)
    {
        return String.format("INSERT INTO %s VALUES(%s) RETURNING id;");
    }
}