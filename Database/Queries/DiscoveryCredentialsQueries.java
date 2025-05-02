package org.nms.Database.Queries;

/**
 * Contains SQL queries for managing discovery credentials associations.
 */
public class DiscoveryCredentialsQueries {

    // CREATE Operations

    /**
     * Creates the discovery_credentials table to associate discovery profiles with credentials.
     *
     * @return SQL query to create the discovery_credentials table
     */
    public static final String CREATE_DISCOVERY_CREDENTIALS_TABLE =
            "CREATE TABLE IF NOT EXISTS discovery_credentials (" +
                    "    discovery_profile_id INTEGER REFERENCES discovery_profiles(id) ON DELETE CASCADE," +
                    "    credential_id INTEGER REFERENCES credential_profiles(id) ON DELETE CASCADE," +
                    "    PRIMARY KEY (discovery_profile_id, credential_id)" +
                    ");";

    /**
     * Associates a credential with a discovery profile in batch mode.
     *
     * @param $1 Discovery profile ID
     * @param $2 Credential ID
     * @return The created association between discovery profile and credential
     */
    public static final String CREATE_DISCOVERY_CREDENTIALS_IN_BATCH_QUERY =
            "INSERT INTO discovery_credentials (discovery_profile_id, credential_id)" +
                    " VALUES ($1, $2)" +
                    "ON CONFLICT (discovery_profile_id, credential_id) DO NOTHING" +
                    " RETURNING discovery_profile_id, credential_id;";


    // DELETE Operations

    /**
     * Removes the association between a discovery profile and a credential.
     *
     * @param $1 Discovery profile ID
     * @param $2 Credential ID
     * @return The deleted association
     */
    public static final String DELETE_DISCOVERY_CREDENTIALS_QUERY =
            "DELETE FROM discovery_credentials dc" +
                    " USING discovery_profiles dp" +
                    " WHERE dc.discovery_profile_id = dp.id" +
                    "   AND dp.status = 'PENDING'" +
                    "   AND dc.discovery_profile_id = $1" +
                    "   AND dc.credential_id = $2" +
                    " RETURNING dc.discovery_profile_id, dc.credential_id;";

}