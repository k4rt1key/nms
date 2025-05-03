package org.nms.Database.Services;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import org.nms.ConsoleLogger;
import org.nms.Database.Queries.CredentialQueries;
import org.nms.HttpServer.Utility.PostgresQuery;

public class CredentialService implements BaseDatabaseService
{

    private CredentialService()
    {
    }

    private static final CredentialService credentialService = new CredentialService();

    public static CredentialService getInstance()
    {
        return credentialService;
    }

    public Future<Void> createSchema()
    {
        ConsoleLogger.debug("Credential Schema Created");
        return PostgresQuery
                .execute(CredentialQueries.CREATE_CREDENTIAL_TYPE)
                .compose(v -> PostgresQuery.execute(CredentialQueries.CREATE_CREDENTIAL_PROFILES_TABLE))
                .mapEmpty();
    }

    @Override
    public Future<JsonArray> get(JsonArray params)
    {
        return PostgresQuery
                .execute(CredentialQueries.GET_WINDOWS_CREDENTIAL_BY_ID_QUERY, params)
                                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> getAll()
    {
        return PostgresQuery
                .execute(CredentialQueries.GET_WINDOWS_CREDENTIALS_QUERY)
                                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> save(JsonArray params)
    {
        return PostgresQuery
                .execute(CredentialQueries.CREATE_WINDOWS_CREDENTIAL_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> update(JsonArray params)
    {
        return PostgresQuery
                .execute(CredentialQueries.UPDATE_WINDOWS_CREDENTIAL_BY_ID_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> delete(JsonArray params)
    {
        return PostgresQuery
                .execute(CredentialQueries.DELETE_WINDOWS_CREDENTIAL_BY_ID_QUERY, params)
                                .map(PostgresQuery::toJsonArray);
    }
}
