package org.nms.Database.Queries;

/**
 * Contains SQL queries for managing discovery profiles.
 */
public class DiscoveryQueries {

    // CREATE Operations

    /**
     * Creates the ip_type enum type for IP address configurations.
     *
     * @return SQL query to create the ip_type enum
     */
    public static final String CREATE_IP_TYPE =
            "DO $$ " +
                    "BEGIN" +
                    "    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'ip_type') THEN" +
                    "        CREATE TYPE ip_type AS ENUM ('SINGLE', 'RANGE', 'SUBNET');" +
                    "    END IF;" +
                    "END" +
                    "$$;";

    /**
     * Creates the discovery_status enum type for discovery profile states.
     *
     * @return SQL query to create the discovery_status enum
     */
    public static final String CREATE_DISCOVERY_STATUS =
            "DO $$ " +
                    "BEGIN" +
                    "    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'discovery_status') THEN" +
                    "        CREATE TYPE discovery_status AS ENUM ('PENDING', 'COMPLETED');" +
                    "    END IF;" +
                    "END" +
                    "$$;";

    /**
     * Creates the discovery_profiles table to store discovery configurations.
     *
     * @return SQL query to create the discovery_profiles table
     */
    public static final String CREATE_DISCOVERY_PROFILES_TABLE =
            "CREATE TABLE IF NOT EXISTS discovery_profiles (" +
                    "    id SERIAL PRIMARY KEY," +
                    "    name VARCHAR(255) UNIQUE NOT NULL," +
                    "    ip VARCHAR(255) NOT NULL," +
                    "    ip_type ip_type NOT NULL," +
                    "    status discovery_status DEFAULT 'PENDING'," +
                    "    port INTEGER" +
                    ");";

    /**
     * Creates a new discovery profile with PENDING status.
     *
     * @param $1 Profile name
     * @param $2 IP address or range
     * @param $3 IP type (SINGLE, RANGE, or SUBNET)
     * @param $4 Port number
     * @return The newly created discovery profile
     */
    public static final String CREATE_DISCOVERY_PROFILE_QUERY =
            "INSERT INTO discovery_profiles (" +
                    "    name," +
                    "    ip," +
                    "    ip_type," +
                    "    status," +
                    "    port" +
                    ") VALUES ($1, $2, $3, 'PENDING', $4)" +
                    " RETURNING id, name, ip, ip_type, status, port;";

    // READ Operations

    /**
     * Retrieves all discovery profiles.
     *
     * @return All discovery profiles
     */
    public static final String GET_ALL_DISCOVERY_PROFILES_QUERY =
            "SELECT * FROM discovery_profiles;";

    /**
     * Retrieves a specific discovery profile by ID.
     *
     * @param $1 Discovery profile ID
     * @return The specified discovery profile if it exists
     */
    public static final String GET_DISCOVERY_PROFILE_BY_ID_QUERY =
            "SELECT * FROM discovery_profiles" +
                    " WHERE id = $1;";

    /**
     * Retrieves all discovery profiles with their associated credentials.
     *
     * @return All discovery profiles with their associated credentials
     */
    public static final String GET_ALL_DISCOVERY_PROFILES_WITH_CREDENTIALS_QUERY =
            "SELECT" +
                    "    dp.id AS id," +
                    "    dp.name AS name," +
                    "    dp.ip AS ip," +
                    "    dp.ip_type AS id_type," +
                    "    dp.status AS status," +
                    "    dp.port AS port," +
                    "    ARRAY_AGG(" +
                    "        JSON_BUILD_OBJECT(" +
                    "            'id', cp.id," +
                    "            'name', cp.name," +
                    "            'username', cp.username," +
                    "            'password', cp.password" +
                    "        )" +
                    "    ) AS credentials" +
                    " FROM discovery_profiles dp" +
                    " LEFT JOIN discovery_credentials dc ON dp.id = dc.discovery_profile_id" +
                    " LEFT JOIN credential_profiles cp ON dc.credential_id = cp.id" +
                    " GROUP BY dp.id, dp.name, dp.ip, dp.ip_type, dp.status, dp.port;";

    /**
     * Retrieves a specific discovery profile with its associated credentials.
     *
     * @param $1 Discovery profile ID
     * @return The specified discovery profile with its credentials if it exists
     */
    public static final String GET_DISCOVERY_PROFILES_WITH_CREDENTIALS_BY_ID_QUERY =
                    "SELECT" +
                    "    dp.id AS id," +
                    "    dp.name AS name," +
                    "    dp.ip AS ip," +
                    "    dp.ip_type AS ip_type," +
                    "    dp.status AS status," +
                    "    dp.port AS port," +
                    "    ARRAY_AGG(" +
                    "        JSON_BUILD_OBJECT(" +
                    "            'id', cp.id," +
                    "            'name', cp.name," +
                    "            'username', cp.username," +
                    "            'password', cp.password" +
                    "        )" +
                    "    ) AS credentials" +
                    " FROM discovery_profiles dp" +
                    " LEFT JOIN discovery_credentials dc ON dp.id = dc.discovery_profile_id" +
                    " LEFT JOIN credential_profiles cp ON dc.credential_id = cp.id" +
                    " WHERE dp.id = $1" +
                    " GROUP BY dp.id, dp.name, dp.ip, dp.ip_type, dp.status, dp.port;";

    /**
     * Retrieves a specific discovery profile with its associated results.
     *
     * @param $1 Discovery profile ID
     * @return The specified discovery profile with its results if it exists
     */
    public static final String GET_DISCOVERY_WITH_RESULTS_BY_ID_QUERY =
            "SELECT" +
                    "    dp.id AS id," +
                    "    dp.name AS name," +
                    "    dp.ip AS ip," +
                    "    dp.ip_type AS ip_type," +
                    "    dp.status AS status," +
                    "    dp.port AS port," +
                    "    COALESCE(" +
                    "        json_agg(" +
                    "            json_build_object(" +
                    "                'ip', dr.ip," +
                    "                'windows_credential', json_build_object(" +
                    "                    'id', cp.id," +
                    "                    'name', cp.name," +
                    "                    'type', cp.type," +
                    "                    'username', cp.username," +
                    "                    'password', cp.password" +
                    "                )," +
                    "                'message', dr.message," +
                    "                'status', dr.status," +
                    "                'created_at', dr.created_at" +
                    "            )" +
                    "        ) FILTER (WHERE dr.ip IS NOT NULL)," +
                    "        '[]'" +
                    "    ) AS results" +
                    " FROM discovery_profiles dp" +
                    " LEFT JOIN discovery_results dr ON dr.discovery_profile_id = dp.id" +
                    " LEFT JOIN credential_profiles cp ON cp.id = dr.credential_id" +
                    " WHERE dp.id = $1" +
                    " GROUP BY dp.id, dp.name, dp.ip, dp.ip_type, dp.status, dp.port" +
                    " ORDER BY dp.id;";

    /**
     * Retrieves all discovery profiles with their associated results.
     *
     * @return All discovery profiles with their results
     */
    public static final String GET_ALL_DISCOVERIES_WITH_RESULTS_QUERY =
            "SELECT" +
                    "    dp.id AS discovery_profile_id," +
                    "    dp.name AS discovery_profile_name," +
                    "    dp.ip," +
                    "    dp.ip_type," +
                    "    dp.status AS discovery_profile_status," +
                    "    dp.port," +
                    "    COALESCE(" +
                    "        json_agg(" +
                    "            json_build_object(" +
                    "                'ip', dr.ip," +
                    "                'windows_credential', json_build_object(" +
                    "                    'id', cp.id," +
                    "                    'name', cp.name," +
                    "                    'type', cp.type," +
                    "                    'username', cp.username," +
                    "                    'password', cp.password" +
                    "                )," +
                    "                'message', dr.message," +
                    "                'status', dr.status," +
                    "                'created_at', dr.created_at" +
                    "            )" +
                    "        ) FILTER (WHERE dr.ip IS NOT NULL)," +
                    "        '[]'" +
                    "    ) AS results" +
                    " FROM discovery_profiles dp" +
                    " LEFT JOIN discovery_results dr ON dr.discovery_profile_id = dp.id" +
                    " LEFT JOIN credential_profiles cp ON cp.id = dr.credential_id" +
                    " GROUP BY dp.id, dp.name, dp.ip, dp.ip_type, dp.status, dp.port" +
                    " ORDER BY dp.id;";

    // UPDATE Operations

    /**
     * Updates a discovery profile if its status is still PENDING.
     *
     * @param $1 New name (or null to keep current)
     * @param $2 New IP address (or null to keep current)
     * @param $3 New IP type (or null to keep current)
     * @param $4 New port (or null to keep current)
     * @param $5 Discovery profile ID
     * @return The updated discovery profile
     */
    public static final String UPDATE_DISCOVERY_PROFILE_IF_NOT_COMPLETED_QUERY =
            "UPDATE discovery_profiles" +
                    " SET" +
                    "    name = COALESCE($2, name)," +
                    "    ip = COALESCE($3, ip)," +
                    "    ip_type = COALESCE($4, ip_type)," +
                    "    port = COALESCE($5, port)" +
                    " WHERE id = $1 AND status = 'PENDING'" +
                    " RETURNING id, name, ip, ip_type, port, status;";

    /**
     * Updates the status of a discovery profile.
     *
     * @param $1 New status
     */

    public static final String UPDATE_DISCOVERY_PROFILE_STATUS_QUERY =
            "UPDATE discovery_profiles" +
                    " SET status = $2" +
                    " WHERE id = $1" +
                    " RETURNING *;";

    // DELETE Operations

    /**
     * Deletes a discovery profile with its associated credentials and results.
     *
     * @param $1 Discovery profile ID
     * @return The deleted discovery profile
     */
    public static final String DELETE_DISCOVERY_PROFILE_WITH_CREDENTIALS_AND_RESULTS_QUERY =
            "DELETE FROM discovery_profiles" +
                    " WHERE id = $1" +
                    " RETURNING id, name, ip, ip_type;";
}