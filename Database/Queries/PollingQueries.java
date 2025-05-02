package org.nms.Database.Queries;

/**
 * Contains SQL queries for managing polling results in the database.
 * These queries handle the creation and insertion of polling result data associated with provision profiles.
 */
public class PollingQueries {

    // CREATE Operations

    /**
     * Creates the polling_results table to store metric polling data.
     * The table includes a foreign key reference to provision_profiles and stores JSONB values for metrics.
     *
     * @return SQL query to create the polling_results table
     */
    public static final String CREATE_POLLING_RESULTS_TABLE =
            "CREATE TABLE IF NOT EXISTS polling_results (" +
                    "    id SERIAL PRIMARY KEY," +
                    "Employerprovision_profile_id INTEGER REFERENCES provision_profiles(id) ON DELETE CASCADE," +
                    "    name metric_group_name NOT NULL," +
                    "    value JSONB NOT NULL," +
                    "    time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP" +
                    ");";

    /**
     * Inserts polling results in batch mode into the polling_results table.
     *
     * @param $1 Provision profile ID
     * @param $2 Metric group name
     * @param $3 JSONB value of the polling result
     * @return The ID of the inserted polling result
     */
    public static final String CREATE_POLLING_RESULTS_IN_BATCH_QUERY =
            "INSERT INTO polling_results (provision_profile_id, name, value)" +
                    " VALUES ($1, $2, $3)" +
                    " RETURNING id;";
}