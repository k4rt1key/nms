package org.nms.Database.Services;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.sqlclient.Tuple;
import org.nms.ConsoleLogger;
import org.nms.Database.Queries.DiscoveryCredentialsQueries;
import org.nms.Database.Queries.DiscoveryQueries;
import org.nms.Database.Queries.DiscoveryResultsQueries;
import org.nms.HttpServer.Utility.PostgresQuery;

import java.util.List;

public class DiscoveryService implements BaseDatabaseService
{

    private DiscoveryService()
    {
    }

    private static final DiscoveryService discoveryService = new DiscoveryService();

    public static DiscoveryService getInstance()
    {
        return discoveryService;
    }

    @Override
    public  Future<Void> createSchema()
    {
        return PostgresQuery
                .execute(DiscoveryQueries.CREATE_IP_TYPE)
                .compose(v -> PostgresQuery
                        .execute(DiscoveryQueries.CREATE_DISCOVERY_PROFILES_TABLE))
                .compose(v -> PostgresQuery
                        .execute(DiscoveryCredentialsQueries.CREATE_DISCOVERY_CREDENTIALS_TABLE))
                .compose(v -> PostgresQuery
                        .execute(DiscoveryResultsQueries.CREATE_DISCOVERY_RESULT_STATUS))
                .compose(v -> PostgresQuery
                        .execute(DiscoveryResultsQueries.CREATE_DISCOVERY_RESULTS_TABLE))
                .mapEmpty();

    }

    @Override
    public Future<JsonArray> get(JsonArray params)
    {
        return PostgresQuery
                .execute(DiscoveryQueries.GET_DISCOVERY_PROFILE_BY_ID_QUERY, params)
                                .map(PostgresQuery::toJsonArray);
    }

    public Future<JsonArray> getWithCredentialsById(JsonArray params)
    {
        return PostgresQuery
                .execute(DiscoveryQueries.GET_DISCOVERY_PROFILES_WITH_CREDENTIALS_BY_ID_QUERY, params)
                                .map(PostgresQuery::toJsonArray);
    }

    public Future<JsonArray> getWithResultsById(JsonArray params)
    {
        return PostgresQuery
                .execute(DiscoveryQueries.GET_DISCOVERY_WITH_RESULTS_BY_ID_QUERY, params)
                                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> getAll()
    {
        return PostgresQuery
                .execute(DiscoveryQueries.GET_ALL_DISCOVERY_PROFILES_QUERY)
                                .map(PostgresQuery::toJsonArray);
    }

    public Future<JsonArray> getAllWithCredentials()
    {
        return PostgresQuery
                .execute(DiscoveryQueries.GET_ALL_DISCOVERY_PROFILES_WITH_CREDENTIALS_QUERY)
                .map(PostgresQuery::toJsonArray);
    }

    public Future<JsonArray> getAllWithResults()
    {
        return PostgresQuery
                .execute(DiscoveryQueries.GET_ALL_DISCOVERIES_WITH_RESULTS_QUERY)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> save(JsonArray params)
    {
        return PostgresQuery
                .execute(DiscoveryQueries.CREATE_DISCOVERY_PROFILE_QUERY, params)
                                .map(PostgresQuery::toJsonArray);
    }

    public Future<JsonArray> saveResults(List<Tuple> params)
    {
        return PostgresQuery
                .execute(DiscoveryResultsQueries.CREATE_DISCOVERY_RESULTS_IN_BATCH_QUERY, params)
                                .map(PostgresQuery::toJsonArray);
    }

    public Future<JsonArray> saveCredentials(List<Tuple> params)
    {
        return PostgresQuery
                .execute(DiscoveryCredentialsQueries.CREATE_DISCOVERY_CREDENTIALS_IN_BATCH_QUERY, params)
                                .map(PostgresQuery::toJsonArray);
    }
    @Override
    public Future<JsonArray> update(JsonArray params)
    {
        return PostgresQuery
                .execute(DiscoveryQueries.UPDATE_DISCOVERY_PROFILE_IF_NOT_COMPLETED_QUERY, params)
                                .map(PostgresQuery::toJsonArray);
    }

    public Future<JsonArray> updateStatus(JsonArray params)
    {
        return PostgresQuery
                .execute(DiscoveryQueries.UPDATE_DISCOVERY_PROFILE_STATUS_QUERY, params)
                                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> delete(JsonArray params)
    {
        return PostgresQuery
                .execute(DiscoveryQueries.DELETE_DISCOVERY_PROFILE_WITH_CREDENTIALS_AND_RESULTS_QUERY, params)
                                .map(PostgresQuery::toJsonArray);
    }

    public Future<JsonArray> deleteCredentials(List<Tuple> params)
    {
        return PostgresQuery
                .execute(DiscoveryCredentialsQueries.DELETE_DISCOVERY_CREDENTIALS_QUERY, params)
                                .map(PostgresQuery::toJsonArray);
    }
}
