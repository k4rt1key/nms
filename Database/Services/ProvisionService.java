package org.nms.Database.Services;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import org.nms.ConsoleLogger;
import org.nms.Database.Queries.ProvisionQueries;
import org.nms.HttpServer.Utility.PostgresQuery;

public class ProvisionService implements BaseDatabaseService {

    private ProvisionService(){

    }

    private static final ProvisionService provisionService = new ProvisionService();

    public static ProvisionService getInstance() {
        return provisionService;
    }

    @Override
    public Future<Void> createSchema() {
        ConsoleLogger.debug("ProvisionServiceImpl => createSchema" + " => Running on " + Thread.currentThread().getName());
        return PostgresQuery
                .execute(ProvisionQueries.CREATE_PROVISION_PROFILES_TABLE)
                .mapEmpty();
    }

    @Override
    public Future<JsonArray> get(JsonArray params) {
        ConsoleLogger.debug("ProvisionServiceImpl => get => " + params + " => Running on " + Thread.currentThread().getName());
        return PostgresQuery
                .execute(ProvisionQueries.GET_PROVISION_BY_ID_QUERY)
                                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> getAll() {
        ConsoleLogger.debug("ProvisionServiceImpl => getAll" + " => Running on " + Thread.currentThread().getName());
        return PostgresQuery
                .execute(ProvisionQueries.GET_PROVISION_QUERY)
                                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> save(JsonArray params) {
        ConsoleLogger.debug("ProvisionServiceImpl => save => " + params + " => Running on " + Thread.currentThread().getName());
        return PostgresQuery
                .execute(ProvisionQueries.CREATE_PROVISION_QUERY,params)
                                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> update(JsonArray params) {
        ConsoleLogger.warn("ProvisionServiceImpl => Update method not implemented" + " => Running on " + Thread.currentThread().getName());

        return null;
    }

    @Override
    public Future<JsonArray> delete(JsonArray params) {
        ConsoleLogger.debug("ProvisionServiceImpl => delete => " + params + " => Running on " + Thread.currentThread().getName());
        return PostgresQuery
                .execute(ProvisionQueries.DELETE_PROVISION_BY_ID_QUERY, params)
                                .map(PostgresQuery::toJsonArray);
    }
}
