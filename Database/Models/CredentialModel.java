package org.nms.Database.Models;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import org.nms.Database.PostgresQuery;

public class CredentialModel implements BaseModel
{

    private CredentialModel()
    {
        // Private constructor
    }

    private static final CredentialModel credentialService = new CredentialModel();

    public static CredentialModel getInstance()
    {
        return credentialService;
    }

    public Future<Void> createSchema()
    {
        var CREATE_CREDENTIAL_TYPE =
                "DO $$ " +
                        "BEGIN" +
                        "    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'credential_type') THEN" +
                        "        CREATE TYPE credential_type AS ENUM ('WINRM', 'SSH', 'SNMPv1', 'SNMPv2c', 'SNMPv3');" +
                        "    END IF;" +
                        "END" +
                        "$$;";

        var CREATE_CREDENTIAL_PROFILES_TABLE =
                "CREATE TABLE IF NOT EXISTS credential_profiles (" +
                        "    id SERIAL PRIMARY KEY," +
                        "    name VARCHAR(255) UNIQUE NOT NULL," +
                        "    type credential_type NOT NULL," +
                        "    username VARCHAR(255)," +
                        "    password VARCHAR(255)," +
                        "    community VARCHAR(255)," +
                        "    auth_protocol VARCHAR(50)," +
                        "    auth_password VARCHAR(255)," +
                        "    privacy_protocol VARCHAR(50)," +
                        "    privacy_password VARCHAR(255)" +
                        ");";

        return PostgresQuery
                .execute(CREATE_CREDENTIAL_TYPE)
                .compose(v -> PostgresQuery.execute(CREATE_CREDENTIAL_PROFILES_TABLE))
                .mapEmpty();
    }

    @Override
    public Future<JsonArray> get(JsonArray params)
    {
        var GET_WINDOWS_CREDENTIAL_BY_ID_QUERY =
                "SELECT id, name, type, username, password" +
                        " FROM credential_profiles" +
                        " WHERE id = $1;";

        return PostgresQuery
                .execute(GET_WINDOWS_CREDENTIAL_BY_ID_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> getAll()
    {
        var GET_WINDOWS_CREDENTIALS_QUERY =
                "SELECT id, name, type, username, password" +
                        " FROM credential_profiles;";

        return PostgresQuery
                .execute(GET_WINDOWS_CREDENTIALS_QUERY)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> save(JsonArray params)
    {
        var CREATE_WINDOWS_CREDENTIAL_QUERY =
                "INSERT INTO credential_profiles (" +
                        "    name," +
                        "    type," +
                        "    username," +
                        "    password," +
                        "    community," +
                        "    auth_protocol," +
                        "    auth_password," +
                        "    privacy_protocol," +
                        "    privacy_password" +
                        ") VALUES ($1, $2, $3, $4, null, null, null, null, null)" +
                        " RETURNING id, name, type, username, password;";

        return PostgresQuery
                .execute(CREATE_WINDOWS_CREDENTIAL_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> update(JsonArray params)
    {
        var UPDATE_WINDOWS_CREDENTIAL_BY_ID_QUERY =
                "UPDATE credential_profiles" +
                        " SET" +
                        "    name = COALESCE($2, name)," +
                        "    type = COALESCE($3, type)," +
                        "    username = COALESCE($4, username)," +
                        "    password = COALESCE($5, password)" +
                        " WHERE id = $1" +
                        " RETURNING id, name, type, username, password;";

        return PostgresQuery
                .execute(UPDATE_WINDOWS_CREDENTIAL_BY_ID_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> delete(JsonArray params)
    {
        var DELETE_WINDOWS_CREDENTIAL_BY_ID_QUERY =
                "DELETE FROM credential_profiles" +
                        " WHERE id = $1" +
                        " RETURNING id, name, type, username, password;";

        return PostgresQuery
                .execute(DELETE_WINDOWS_CREDENTIAL_BY_ID_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }
}
