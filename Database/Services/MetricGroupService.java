package org.nms.Database.Services;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import org.nms.ConsoleLogger;
import org.nms.Database.Queries.MetricGroupsQueries;
import org.nms.HttpServer.Utility.PostgresQuery;

public class MetricGroupService implements BaseDatabaseService {

    private MetricGroupService(){

    }

    private static final MetricGroupService metricGroupService = new MetricGroupService();

    public static MetricGroupService getInstance() {
        return metricGroupService;
    }

    @Override
    public Future<Void> createSchema() {
        ConsoleLogger.debug("MetricGroupServiceImpl => createSchema" + " => Running on " + Thread.currentThread().getName());
        return PostgresQuery
                .execute(MetricGroupsQueries.CREATE_METRIC_GROUP_NAMES)
                .compose(v -> PostgresQuery
                        .execute(MetricGroupsQueries.CREATE_METRIC_GROUP_TABLE))
                .mapEmpty();
    }

    @Override
    public Future<JsonArray> get(JsonArray params) {
        ConsoleLogger.debug("MetricGroupServiceImpl => get => " + params + " => Running on " + Thread.currentThread().getName());
        return PostgresQuery
                .execute(MetricGroupsQueries.GET_METRIC_GROUPS_BY_PROVISION_ID_AND_NAME_QUERY, params)
                                .map(PostgresQuery::toJsonArray);
    }

    public Future<JsonArray> getByName(JsonArray params) {
        ConsoleLogger.debug("MetricGroupServiceImpl => getByName => " + params + " => Running on " + Thread.currentThread().getName());
        return PostgresQuery
                .execute(MetricGroupsQueries.GET_METRIC_GROUPS_BY_PROVISION_ID_AND_NAME_QUERY, params)
                                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> getAll() {
        ConsoleLogger.warn("MetricGroupServiceImpl => getAll method not implemented" + " => Running on " + Thread.currentThread().getName());

        return null;
    }

    @Override
    public Future<JsonArray> save(JsonArray params) {
        ConsoleLogger.debug("MetricGroupServiceImpl => save => " + params + " => Running on " + Thread.currentThread().getName());
        return PostgresQuery
                .execute(MetricGroupsQueries.ADD_METRIC_GROUP_TO_PROVISION_QUERY, params)
                                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> update(JsonArray params) {
        ConsoleLogger.debug("MetricGroupServiceImpl => update => " + params + " => Running on " + Thread.currentThread().getName());
        return PostgresQuery
                .execute(MetricGroupsQueries.UPDATE_METRIC_GROUP_QUERY, params)
                                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> delete(JsonArray params) {
        ConsoleLogger.debug("MetricGroupServiceImpl => delete => " + params + " => Running on " + Thread.currentThread().getName());
        return PostgresQuery
                .execute(MetricGroupsQueries.DELETE_METRIC_GROUP_QUERY, params)
                                .map(PostgresQuery::toJsonArray);
    }
}
