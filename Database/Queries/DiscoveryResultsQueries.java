package org.nms.Database.Queries;

/**
 * Contains SQL queries for managing discovery results.
 */
public class DiscoveryResultsQueries {

    // CREATE Operations

    /**
     * Creates the discovery_result_status enum type.
     *
     * @return SQL query to create the discovery_result_status enum
     */
    public static final String CREATE_DISCOVERY_RESULT_STATUS =
            "DO $$ " +
                    "BEGIN" +
                    "    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'discovery_result_status') THEN" +
                    "        CREATE TYPE discovery_result_status AS ENUM ('COMPLETED', 'FAIL');" +
                    "    END IF;" +
                    "END" +
                    "$$;";

    /**
     * Creates the discovery_results table to store discovery outcomes.
     *
     * @return SQL query to create the discovery_results table
     */
    public static final String CREATE_DISCOVERY_RESULTS_TABLE =
            "CREATE TABLE IF NOT EXISTS discovery_results (" +
                    "    discovery_profile_id INTEGER REFERENCES discovery_profiles(id) ON DELETE CASCADE," +
                    "    ip VARCHAR(255) NOT NULL," +
                    "    credential_id INTEGER REFERENCES credential_profiles(id) ON DELETE RESTRICT," +
                    "    message TEXT," +
                    "    status discovery_result_status NOT NULL," +
                    "    PRIMARY KEY (discovery_profile_id, ip)," +
                    "    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP" +
                    ");";

    /**
     * Creates discovery results in batch mode.
     *
     * @param $1 Discovery profile ID
     * @param $2 IP address
     * @param $3 Credential ID
     * @param $4 Message or result details
     * @param $5 Status of the discovery (COMPLETED, FAILED, etc.)
     * @return The newly created discovery result
     */
    public static final String CREATE_DISCOVERY_RESULTS_IN_BATCH_QUERY =
            "INSERT INTO discovery_results (" +
                    "    discovery_profile_id," +
                    "    ip," +
                    "    credential_id," +
                    "    message," +
                    "    status" +
                    ") VALUES ($1, $2, $3, $4, $5)" +
                    "ON CONFLICT (discovery_profile_id, ip) DO NOTHING" +
                    " RETURNING *;";


}