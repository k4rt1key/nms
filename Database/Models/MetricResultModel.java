package org.nms.Database.Models;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.sqlclient.Tuple;
import org.nms.Database.PostgresQuery;

import java.util.List;

public class MetricResultModel implements BaseModel
{

    private MetricResultModel()
    {
        // Private constructor
    }

    private static final MetricResultModel polledDataService = new MetricResultModel();

    public static MetricResultModel getInstance()
    {
        return polledDataService;
    }

    @Override
    public Future<Void> createSchema()
    {
        var CREATE_POLLING_RESULTS_TABLE =
                "CREATE TABLE IF NOT EXISTS polling_results (" +
                        "    id SERIAL PRIMARY KEY," +
                        "    provision_profile_id INTEGER REFERENCES provision_profiles(id) ON DELETE CASCADE," +
                        "    name metric_group_name NOT NULL," +
                        "    value JSONB NOT NULL," +
                        "    time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP" +
                        ");";

        return PostgresQuery
                .execute(CREATE_POLLING_RESULTS_TABLE)
                .mapEmpty();
    }

    @Override
    public Future<JsonArray> get(JsonArray params)
    {
        return Future.succeededFuture(new JsonArray());
    }

    @Override
    public Future<JsonArray> getAll()
    {
        String GET_ALL_POLLED_DATA = "SELECT * FROM polling_results;";
        return PostgresQuery.execute(GET_ALL_POLLED_DATA)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> save(JsonArray params)
    {
        var CREATE_POLLING_RESULTS_IN_BATCH_QUERY =
                "INSERT INTO polling_results (provision_profile_id, name, value)" +
                        " VALUES ($1, $2, $3)" +
                        " RETURNING id;";

        return PostgresQuery
                .execute(CREATE_POLLING_RESULTS_IN_BATCH_QUERY)
                .map(PostgresQuery::toJsonArray);
    }

    public Future<JsonArray> save(List<Tuple> params)
    {
        var CREATE_POLLING_RESULTS_IN_BATCH_QUERY =
                "INSERT INTO polling_results (provision_profile_id, name, value)" +
                        " VALUES ($1, $2, $3)" +
                        " RETURNING id;";

        return PostgresQuery
                .execute(CREATE_POLLING_RESULTS_IN_BATCH_QUERY, params)
                .map(PostgresQuery::toJsonArray);
    }

    @Override
    public Future<JsonArray> update(JsonArray params)
    {
        return Future.failedFuture("Update not allowed");
    }

    @Override
    public Future<JsonArray> delete(JsonArray params)
    {
        return Future.failedFuture("Delete not allowed");
    }
}
