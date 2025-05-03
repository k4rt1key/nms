package org.nms.Database.Services;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.sqlclient.Tuple;
import org.nms.ConsoleLogger;
import org.nms.Database.Queries.PollingQueries;
import org.nms.HttpServer.Utility.PostgresQuery;

import java.util.List;

public class PolledDataService implements BaseDatabaseService {

    private PolledDataService(){

    }

    private static final PolledDataService polledDataService = new PolledDataService();

    public static PolledDataService getInstance() {
        return polledDataService;
    }

    @Override
    public Future<Void> createSchema() {
        ConsoleLogger.debug("PolledDataServiceImpl => createSchema" + " => Running on " + Thread.currentThread().getName());
        return PostgresQuery
                .execute(PollingQueries.CREATE_POLLING_RESULTS_TABLE)
                .mapEmpty();
    }

    @Override
    public Future<JsonArray> get(JsonArray params) {
        ConsoleLogger.warn("PolledDataServiceImpl => get method not implemented" + " => Running on " + Thread.currentThread().getName());
        return null;
    }

    @Override
    public Future<JsonArray> getAll() {
        ConsoleLogger.warn("PolledDataServiceImpl => getAll method not implemented" + " => Running on " + Thread.currentThread().getName());
        return null;
    }

    @Override
    public Future<JsonArray> save(JsonArray params) {
        ConsoleLogger.debug("PolledDataServiceImpl => save => " + params + " => Running on " + Thread.currentThread().getName());
        return PostgresQuery
                .execute(PollingQueries.CREATE_POLLING_RESULTS_IN_BATCH_QUERY)
                                .map(PostgresQuery::toJsonArray);
    }

    public Future<JsonArray> save(List<Tuple> params) {
        ConsoleLogger.debug("PolledDataServiceImpl => save => " + params + " => Running on " + Thread.currentThread().getName());
        return PostgresQuery
                .execute(PollingQueries.CREATE_POLLING_RESULTS_IN_BATCH_QUERY)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> update(JsonArray params) {
        ConsoleLogger.warn("PolledDataServiceImpl => Update method not implemented" + " => Running on " + Thread.currentThread().getName());

        return null;
    }

    @Override
    public Future<JsonArray> delete(JsonArray params) {
        ConsoleLogger.warn("PolledDataServiceImpl => Delete method not implemented" + " => Running on " + Thread.currentThread().getName());

        return null;
    }
}
