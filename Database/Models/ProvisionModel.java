package org.nms.Database.Models;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import org.nms.Database.PostgresQuery;

public class ProvisionModel implements BaseModel
{

    private ProvisionModel()
    {
        // Private constructor
    }

    private static final ProvisionModel provisionService = new ProvisionModel();

    public static ProvisionModel getInstance()
    {
        return provisionService;
    }

    @Override
    public Future<Void> createSchema()
    {
        var CREATE_PROVISION_PROFILES_TABLE =
                "CREATE TABLE IF NOT EXISTS provision_profiles (" +
                        "    id SERIAL PRIMARY KEY," +
                        "    ip VARCHAR(255) UNIQUE NOT NULL," +
                        "    port INTEGER," +
                        "    credential_id INTEGER REFERENCES credential_profiles(id) ON DELETE RESTRICT" +
                        ");";

        return PostgresQuery
                .execute(CREATE_PROVISION_PROFILES_TABLE)
                .mapEmpty();
    }

    @Override
    public Future<JsonArray> get(JsonArray params)
    {
        var GET_PROVISION_BY_ID_QUERY =
                "SELECT p.id, p.ip, p.port, p.credential_id," +
                        "       json_agg(json_build_object(" +
                        "    'id', m.id, " +

                        "           'provision_profile_id', m.provision_profile_id," +
                        "           'name', m.name," +
                        "           'polling_interval', m.polling_interval" +
                        "       )) AS metric_groups" +
                        " FROM provision_profiles p" +
                        " LEFT JOIN metric_groups m ON p.id = m.provision_profile_id" +
                        " WHERE p.id = $1" +
                        " GROUP BY p.id;";

        return PostgresQuery
                .execute(GET_PROVISION_BY_ID_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> getAll()
    {
        var GET_PROVISION_QUERY =
                "SELECT p.id, p.ip, p.port, " +
                        "json_build_object(" +
                        "    'id', c.id, " +
                        "    'username', c.username, " +
                        "    'password', c.password" +
                        ") AS credential, " +
                        "json_agg(json_build_object(" +
                        "    'id', m.id, " +
                        "    'provision_profile_id', m.provision_profile_id, " +
                        "    'name', m.name, " +
                        "    'polling_interval', m.polling_interval" +
                        ")) AS metric_groups " +
                        "FROM provision_profiles p " +
                        "LEFT JOIN credential_profiles c ON p.credential_id = c.id " +
                        "LEFT JOIN metric_groups m ON p.id = m.provision_profile_id " +
                        "GROUP BY p.id, c.id;";

        return PostgresQuery
                .execute(GET_PROVISION_QUERY)
                .map(PostgresQuery::toJsonArray);

    }

    @Override
    public Future<JsonArray> save(JsonArray params)
    {
        var CREATE_PROVISION_QUERY =
                "WITH discovery_validation AS (" +
                        "    SELECT dr.ip, dr.credential_id, dp.port " +
                        "    FROM discovery_results dr " +
                        "    JOIN discovery_profiles dp ON dr.discovery_profile_id = dp.id " +
                        "    WHERE dr.discovery_profile_id = $1 " +
                        "    AND dr.ip = $2 " +
                        "    AND dr.status = 'COMPLETED'" +
                        "), " +
                        "inserted_provision AS (" +
                        "    INSERT INTO provision_profiles ( " +
                        "        ip, " +
                        "        port, " +
                        "        credential_id " +
                        "    ) " +
                        "    SELECT ip, port, credential_id " +
                        "    FROM discovery_validation " +
                        "    RETURNING *" +
                        "), " +
                        "inserted_metrics AS (" +
                        "    INSERT INTO metric_groups (provision_profile_id, name, polling_interval) " +
                        "    SELECT p.id, m.metric_group_name, 30 " +
                        "    FROM inserted_provision p, " +
                        "         (SELECT unnest(enum_range(NULL::metric_group_name)) AS metric_group_name) m " +
                        "    RETURNING *" +
                        ") " +
                        "SELECT " +
                        "    p.*, " +
                        "    json_build_object( " +
                        "        'id', c.id, " +
                        "        'username', c.username, " +
                        "        'password', c.password " +
                        "    ) AS credential, " +
                        "    COALESCE( " +
                        "        (SELECT json_agg( " +
                        "            json_build_object( " +
                        "                'id', m.id, " +
                        "                'provision_profile_id', m.provision_profile_id, " +
                        "                'name', m.name, " +
                        "                'polling_interval', m.polling_interval " +
                        "            ) " +
                        "        ) " +
                        "        FROM inserted_metrics m " +
                        "        WHERE m.provision_profile_id = p.id), " +
                        "        '[]'::json " +
                        "    ) AS metric_groups " +
                        "FROM inserted_provision p " +
                        "JOIN credential_profiles c ON p.credential_id = c.id;";

        return PostgresQuery
                .execute(CREATE_PROVISION_QUERY,params)
                                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> update(JsonArray params)
    {
        var UPDATE_METRIC_GROUP_QUERY =
                "UPDATE metric_groups" +
                        " SET polling_interval = COALESCE($2, polling_interval) "+
                        " SET enable = COALESCE($4, enable)" +
                        " WHERE provision_profile_id = $1 AND name = $3 AND EXISTS (" +
                        "     SELECT 1 FROM provision_profiles p" +
                        "     WHERE p.id = metric_groups.provision_profile_id" +
                        " )" +
                        " RETURNING *;";

        return PostgresQuery
                .execute(UPDATE_METRIC_GROUP_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> delete(JsonArray params)
    {
        var DELETE_PROVISION_BY_ID_QUERY =
                "DELETE FROM provision_profiles" +
                        " WHERE id = $1" +
                        " RETURNING *;";

        return PostgresQuery
                .execute(DELETE_PROVISION_BY_ID_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }
}
