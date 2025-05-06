package org.nms.Database.Models;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import org.nms.Database.PostgresQuery;

public class UserModel implements BaseModel
{

    private UserModel()
    {
        // Private constructor
    }

    private static final UserModel userService = new UserModel();

    public static UserModel getInstance()
    {
        return userService;
    }

    @Override
    public Future<Void> createSchema()
    {
        var CREATE_USERS_TABLE =
                "CREATE TABLE IF NOT EXISTS users (" +
                        "    id SERIAL PRIMARY KEY," +
                        "    name VARCHAR(255) UNIQUE NOT NULL," +
                        "    email VARCHAR(255) UNIQUE NOT NULL," +
                        "    password VARCHAR(255) NOT NULL" +
                        ");";

        return PostgresQuery
                .execute(CREATE_USERS_TABLE)
                .mapEmpty();
    }

    @Override
    public Future<JsonArray> get(JsonArray params)
    {
        var GET_USER_BY_ID_QUERY =
                "SELECT * FROM users" +
                        " WHERE id = $1;";

        return PostgresQuery
                .execute(GET_USER_BY_ID_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }

    public Future<JsonArray> getByName(JsonArray params)
    {
        var GET_USER_BY_NAME_QUERY =
                "SELECT * FROM users" +
                        " WHERE name = $1;";

        return PostgresQuery
                .execute(GET_USER_BY_NAME_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> getAll()
    {
        var GET_ALL_USERS_QUERY =
                "SELECT * FROM USERS;";

        return PostgresQuery
                .execute(GET_ALL_USERS_QUERY)
                .map(PostgresQuery::toJsonArray);
    }


    @Override
    public Future<JsonArray> save(JsonArray params)
    {
        var CREATE_USER_QUERY =
                "INSERT INTO users (name, email, password)" +
                        " VALUES ($1, $2, $3)" +
                        " RETURNING id, name, email;";

        return PostgresQuery
                .execute(CREATE_USER_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> update(JsonArray params)
    {
        var UPDATE_USER_BY_ID =
                "UPDATE users" +
                        " SET" +
                        "    name = COALESCE($2, name)," +
                        "    email = COALESCE($3, email)," +
                        "    password = COALESCE($4, password)" +
                        " WHERE id = $1" +
                        " RETURNING id, name, email;";

        return PostgresQuery
                .execute(UPDATE_USER_BY_ID, params)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> delete(JsonArray params)
    {
        var DELETE_USER_BY_ID =
                "DELETE FROM users" +
                        " WHERE id = $1" +
                        " RETURNING id, name, email;";

        return PostgresQuery
                .execute(DELETE_USER_BY_ID, params)
                .map(PostgresQuery::toJsonArray);
    }
}
