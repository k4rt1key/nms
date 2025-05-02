package org.nms.Database.Queries;

/**
 * Contains SQL queries for managing user data.
 */
public class UserQueries {

    // CREATE Operations

    /**
     * Creates the users table to store user information.
     *
     */
    public static final String CREATE_USERS_TABLE =
            "CREATE TABLE IF NOT EXISTS users (" +
                    "    id SERIAL PRIMARY KEY," +
                    "    name VARCHAR(255) UNIQUE NOT NULL," +
                    "    email VARCHAR(255) UNIQUE NOT NULL," +
                    "    password VARCHAR(255) NOT NULL" +
                    ");";

    /**
     * Creates a new user in the system.
     *
     * @param $1 User's name
     * @param $2 User's email
     * @param $3 User's password (should be hashed before being stored)
     * @return The newly created user without the password field
     */
    public static final String CREATE_USER_QUERY =
            "INSERT INTO users (name, email, password)" +
                    " VALUES ($1, $2, $3)" +
                    " RETURNING id, name, email;";

    // READ Operations

    /**
     * Get ALL Users
     */

    public static final String GET_ALL_USERS_QUERY =
            "SELECT * FROM USERS;";

    /**
     * Retrieves a user by their ID.
     *
     * @param $1 User ID
     * @return The user with the specified ID without the password field
     */
    public static final String GET_USER_BY_ID_QUERY =
            "SELECT * FROM users" +
                    " WHERE id = $1;";

    /**
     * Retrieves a user by their name.
     *
     * @param $1 User's name
     * @return The user with the specified name without the password field
     */
    public static final String GET_USER_BY_NAME_QUERY =
            "SELECT * FROM users" +
                    " WHERE name = $1;";

    // UPDATE Operations

    /**
     * Updates a user's information.
     *
     * @param $1 User ID
     * @param $2 New name (or null to keep current)
     * @param $3 New email (or null to keep current)
     * @param $4 New password (or null to keep current)
     * @return The updated user without the password field
     */
    public static final String UPDATE_USER_BY_ID =
            "UPDATE users" +
                    " SET" +
                    "    name = COALESCE($2, name)," +
                    "    email = COALESCE($3, email)," +
                    "    password = COALESCE($4, password)" +
                    " WHERE id = $1" +
                    " RETURNING id, name, email;";

    // DELETE Operations

    /**
     * Deletes a user by their ID.
     *
     * @param $1 User ID
     * @return The deleted user without the password field
     */
    public static final String DELETE_USER_BY_ID =
            "DELETE FROM users" +
                    " WHERE id = $1" +
                    " RETURNING id, name, email;";
}