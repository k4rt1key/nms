package org.nms.Database.Services;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.sqlclient.Row;
import org.nms.ConsoleLogger;
import org.nms.Database.Queries.UserQueries;
import org.nms.HttpServer.Utility.PostgresQuery;

public class UserService implements BaseDatabaseService
{

    private UserService()
    {
    }

    private static final UserService userService = new UserService();

    public static UserService getInstance()
    {
        return userService;
    }

    @Override
    public Future<Void> createSchema()
    {
        ConsoleLogger.debug("User Schema created");
        return PostgresQuery
                .execute(UserQueries.CREATE_USERS_TABLE)
                .mapEmpty();
    }

    @Override
    public Future<JsonArray> get(JsonArray params)
    {
        return PostgresQuery
                .execute(UserQueries.GET_USER_BY_ID_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }

    public Future<JsonArray> getByName(JsonArray params)
    {
        return PostgresQuery
                .execute(UserQueries.GET_USER_BY_NAME_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> getAll()
    {
        return PostgresQuery
                .execute(UserQueries.GET_ALL_USERS_QUERY)
                .map(PostgresQuery::toJsonArray);
    }


    @Override
    public Future<JsonArray> save(JsonArray params)
    {
        return PostgresQuery
                .execute(UserQueries.CREATE_USER_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> update(JsonArray params)
    {
        return PostgresQuery
                .execute(UserQueries.UPDATE_USER_BY_ID, params)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> delete(JsonArray params)
    {
        return PostgresQuery
                .execute(UserQueries.DELETE_USER_BY_ID, params)
                .map(PostgresQuery::toJsonArray);
    }
}
