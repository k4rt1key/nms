package org.nms.Database.Queries;

/**
 * Contains SQL queries for managing provision profiles.
 */
public class ProvisionQueries {

    // CREATE Operations

    /**
     * Creates the provision_profiles table to store provision configurations.
     *
     * @return SQL query to create the provision_profiles table
     */
    public static final String CREATE_PROVISION_PROFILES_TABLE =
            "CREATE TABLE IF NOT EXISTS provision_profiles (" +
                    "    id SERIAL PRIMARY KEY," +
                    "    ip VARCHAR(255) UNIQUE NOT NULL," +
                    "    port INTEGER," +
                    "    credential_id INTEGER REFERENCES credential_profiles(id) ON DELETE RESTRICT" +
                    ");";

    /**
     * Creates a new provision profile based on a completed discovery result.
     *
     * @param $1 Discovery profile ID
     * @param $2 IP address
     * @return Newly created provision profile with its associated metric_groups
     */
    public static final String CREATE_PROVISION_QUERY =
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

    // READ Operations

    /**
     * Retrieves all provision profiles.
     *
     * @return All provision profiles with their associated metric_groups
     */
    public static final String GET_PROVISION_QUERY =
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

    /**
     * Retrieves a specific provision profile with its associated metric_groups.
     *
     * @param $1 Provision profile ID
     * @return The specified provision profile with its metric_groups if it exists
     */
    public static final String GET_PROVISION_BY_ID_QUERY =
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

    // DELETE Operations

    /**
     * Deletes a provision profile by its ID.
     *
     * @param $1 Provision profile ID
     * @return The deleted provision profile
     */
    public static final String DELETE_PROVISION_BY_ID_QUERY =
            "DELETE FROM provision_profiles" +
                    " WHERE id = $1" +
                    " RETURNING *;";
}