package org.nms.Database.Queries;

/**
 * Contains SQL queries for managing metric groups associated with provision profiles.
 */
public class MetricGroupsQueries {

    // CREATE Operations

    /**
     * Creates the metric_group_name enum type.
     *
     * @return SQL query to create the metric_group_name enum
     */
    public static final String CREATE_METRIC_GROUP_NAMES =
            "DO $$ " +
                    "BEGIN" +
                    "    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'metric_group_name') THEN" +
                    "        CREATE TYPE metric_group_name AS ENUM ('CPU', 'MEMORY', 'DISK', 'FILE', 'PROCESS', 'NETWORK', 'PING', 'SYSTEMINFO');" +
                    "    END IF;" +
                    "END" +
                    "$$;";

    /**
     * Creates the metric_groups table to store metric group configurations.
     *
     * @return SQL query to create the metric_groups table
     */
    public static final String CREATE_METRIC_GROUP_TABLE =
            "CREATE TABLE IF NOT EXISTS metric_groups (" +
                    "    id SERIAL PRIMARY KEY, " +
                    "    provision_profile_id INTEGER REFERENCES provision_profiles(id) ON DELETE CASCADE," +
                    "    name metric_group_name NOT NULL," +
                    "    polling_interval INTEGER NOT NULL," +
                    "    UNIQUE (provision_profile_id, name)" +
                    ");";

    /**
     * Adds a new metric_group to an existing provision profile.
     *
     * @param $1 Provision profile ID
     * @param $2 Metric group's name
     * @param $3 Polling interval
     * @return The newly added metric_group
     */
    public static final String ADD_METRIC_GROUP_TO_PROVISION_QUERY =
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

    // READ Operations

    /**
     * Retrieves all metric_groups associated with a specific provision profile.
     *
     * @param $1 Provision profile ID
     * @return All metric_groups for the specified provision profile
     */
    public static final String GET_METRIC_GROUPS_BY_PROVISION_ID_QUERY =
            "SELECT * FROM metric_groups" +
                    " WHERE provision_profile_id = $1;";

    /**
     * Retrieves a specific metric_group by name for a given provision profile.
     *
     * @param $1 Provision profile ID
     * @param $2 Metric group's name
     * @return The specified metric_group if it exists
     */
    public static final String GET_METRIC_GROUPS_BY_PROVISION_ID_AND_NAME_QUERY =
            "SELECT * FROM metric_groups" +
                    " WHERE provision_profile_id = $1 AND name = $2;";

    /**
     * Retrieves all available metric_groups from the metric_group_names enum.
     *
     * @return All possible metric group's names defined in the system
     */
    public static final String GET_ALL_METRIC_GROUPS_NAME_QUERY =
            "SELECT unnest(enum_range(NULL::metric_group_name)) AS available_metric_groups;";

    // UPDATE Operations

    /**
     * Updates the polling interval for a specific metric_group in a provision profile.
     *
     * @param $1 Provision profile ID
     * @param $2 New polling interval
     * @param $3 Metric group's name
     * @param $4 Enable
     * @return The updated metric_group
     */
    public static final String UPDATE_METRIC_GROUP_QUERY =
            "UPDATE metric_groups" +
                    " SET polling_interval = COALESCE($2, polling_interval) "+
//                    " SET enable = COALESCE($4, enable)" +
                    " WHERE provision_profile_id = $1 AND name = $3 AND EXISTS (" +
                    "     SELECT 1 FROM provision_profiles p" +
                    "     WHERE p.id = metric_groups.provision_profile_id" +
                    " )" +
                    " RETURNING *;";

    // DELETE Operations

    /**
     * Deletes a specific metric_group from a provision profile.
     *
     * @param $1 Provision profile ID
     * @param $2 Metric group's name
     * @return The deleted metric_group
     */
    public static final String DELETE_METRIC_GROUP_QUERY =
            "DELETE FROM metric_groups" +
                    " WHERE provision_profile_id = $1 AND name = $2 AND EXISTS (" +
                    "     SELECT 1 FROM provision_profiles p" +
                    "     WHERE p.id = metric_groups.provision_profile_id" +
                    " )" +
                    " RETURNING *;";
}