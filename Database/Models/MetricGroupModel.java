package org.nms.Database.Models;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import org.nms.Database.PostgresQuery;

public class MetricGroupModel implements BaseModel
{

    private MetricGroupModel()
    {
        // Private constructor
    }

    private static final MetricGroupModel metricGroupService = new MetricGroupModel();

    public static MetricGroupModel getInstance()
    {
        return metricGroupService;
    }

    @Override
    public Future<Void> createSchema()
    {
        var CREATE_METRIC_GROUP_NAMES =
                "DO $$ " +
                        "BEGIN" +
                        "    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'metric_group_name') THEN" +
                        "        CREATE TYPE metric_group_name AS ENUM ('CPUINFO', 'CPUUSAGE', 'UPTIME', 'MEMORY', 'DISK', 'PROCESS', 'NETWORK', 'SYSTEMINFO');" +
                        "    END IF;" +
                        "END" +
                        "$$;";

        var CREATE_METRIC_GROUP_TABLE =
                "CREATE TABLE IF NOT EXISTS metric_groups (" +
                        "    id SERIAL PRIMARY KEY, " +
                        "    provision_profile_id INTEGER REFERENCES provision_profiles(id) ON DELETE CASCADE," +
                        "    name metric_group_name NOT NULL," +
                        "    polling_interval INTEGER NOT NULL," +
                        "    UNIQUE (provision_profile_id, name)" +
                        ");";

        return PostgresQuery
                .execute(CREATE_METRIC_GROUP_NAMES)
                .compose(v -> PostgresQuery.execute(CREATE_METRIC_GROUP_TABLE))
                .mapEmpty();
    }

    @Override
    public Future<JsonArray> get(JsonArray params)
    {
        var GET_METRIC_GROUPS_BY_PROVISION_ID_AND_NAME_QUERY =
                "SELECT * FROM metric_groups" +
                        " WHERE provision_profile_id = $1 AND name = $2;";

        return PostgresQuery
                .execute(GET_METRIC_GROUPS_BY_PROVISION_ID_AND_NAME_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }

    public Future<JsonArray> getByName(JsonArray params)
    {
        var GET_METRIC_GROUPS_BY_PROVISION_ID_AND_NAME_QUERY =
                "SELECT * FROM metric_groups" +
                        " WHERE provision_profile_id = $1 AND name = $2;";

        return PostgresQuery
                .execute(GET_METRIC_GROUPS_BY_PROVISION_ID_AND_NAME_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> getAll()
    {
        return Future.failedFuture("Get all not allowed");
    }

    @Override
    public Future<JsonArray> save(JsonArray params)
    {
        var ADD_METRIC_GROUP_TO_PROVISION_QUERY =
                "INSERT INTO metric_groups (provision_profile_id, name, polling_interval)" +
                        " SELECT $1, $2, $3" +
                        " WHERE EXISTS (" +
                        "     SELECT 1 FROM provision_profiles" +
                        "     WHERE id = $1" +
                        " ) AND NOT EXISTS (" +
                        "     SELECT 1 FROM metric_groups" +
                        "     WHERE provision_profile_id = $1 AND name = $2" +
                        " )" +
                        " RETURNING *;";

        return PostgresQuery
                .execute(ADD_METRIC_GROUP_TO_PROVISION_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> update(JsonArray params)
    {
        var  UPDATE_METRIC_GROUP_QUERY =
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
        var DELETE_METRIC_GROUP_QUERY =
                "DELETE FROM metric_groups" +
                        " WHERE provision_profile_id = $1 AND name = $2 AND EXISTS (" +
                        "     SELECT 1 FROM provision_profiles p" +
                        "     WHERE p.id = metric_groups.provision_profile_id" +
                        " )" +
                        " RETURNING *;";

        return PostgresQuery
                .execute(DELETE_METRIC_GROUP_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }
}
