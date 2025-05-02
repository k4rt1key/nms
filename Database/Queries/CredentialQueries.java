package org.nms.Database.Queries;

/**
 * Contains SQL queries for managing credential profiles.
 */
public class CredentialQueries {

    // CREATE Operations

    /**
     * Creates the credential_type enum type.
     *
     * @return SQL query to create the credential_type enum
     */
    public static final String CREATE_CREDENTIAL_TYPE =
            "DO $$ " +
                    "BEGIN" +
                    "    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'credential_type') THEN" +
                    "        CREATE TYPE credential_type AS ENUM ('WINRM', 'SSH', 'SNMPv1', 'SNMPv2c', 'SNMPv3');" +
                    "    END IF;" +
                    "END" +
                    "$$;";

    /**
     * Creates the credential_profiles table to store credential configurations.
     *
     * @return SQL query to create the credential_profiles table
     */
    public static final String CREATE_CREDENTIAL_PROFILES_TABLE =
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

    /**
     * Creates a new Windows credential profile.
     *
     * @param $1 Credential name
     * @param $2 Credential type (likely WINRM or SSH)
     * @param $3 Username
     * @param $4 Password
     * @return The newly created credential profile
     */
    public static final String CREATE_WINDOWS_CREDENTIAL_QUERY =
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

    // READ Operations

    /**
     * Retrieves all Windows credential profiles.
     *
     * @return All Windows credential profiles
     */
    public static final String GET_WINDOWS_CREDENTIALS_QUERY =
            "SELECT id, name, type, username, password" +
                    " FROM credential_profiles;";

    /**
     * Retrieves a specific Windows credential profile by ID.
     *
     * @param $1 Credential profile ID
     * @return The specified credential profile if it exists
     */
    public static final String GET_WINDOWS_CREDENTIAL_BY_ID_QUERY =
            "SELECT id, name, type, username, password" +
                    " FROM credential_profiles" +
                    " WHERE id = $1;";

    // UPDATE Operations

    /**
     * Updates a Windows credential profile by ID.
     *
     * @param $1 Credential profile ID
     * @param $2 New name (or null to keep current)
     * @param $3 New type (or null to keep current)
     * @param $4 New username (or null to keep current)
     * @param $5 New password ( or null to keep current)
     * @return The updated credential profile
     */
    public static final String UPDATE_WINDOWS_CREDENTIAL_BY_ID_QUERY =
            "UPDATE credential_profiles" +
                    " SET" +
                    "    name = COALESCE($2, name)," +
                    "    type = COALESCE($3, type)," +
                    "    username = COALESCE($4, username)," +
                    "    password = COALESCE($5, password)" +
                    " WHERE id = $1" +
                    " RETURNING id, name, type, username, password;";

    // DELETE Operations

    /**
     * Deletes a credential profile by ID.
     *
     * @param $1 Credential profile ID
     * @return The deleted credential profile
     */
    public static final String DELETE_WINDOWS_CREDENTIAL_BY_ID_QUERY =
            "DELETE FROM credential_profiles" +
                    " WHERE id = $1" +
                    " RETURNING id, name, type, username, password;";
}