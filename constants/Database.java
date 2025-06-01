package org.nms.constants;

public class Database
{
    public static class Common
    {
        public static final String ID = "id";

        public static final String OPERATION = "operation";

        public static final String COLUMN = "column";

        public static final String VALUE = "value";

        public static final String CONDITION = "condition";

        public static final String TABLE_NAME = "table.name";

        public static final String DATA = "data";

        public static final String BATCH_DATA = "batch.data";
    }

    public static class Operation
    {
        public static final String LIST = "list";

        public static final String GET = "get";

        public static final String INSERT = "insert";

        public static final String BATCH_INSERT = "batch.insert";

        public static final String UPDATE = "update";

        public static final String DELETE = "delete";

        public static final String CREATE_SCHEMA = "create.schema";
    }

    public static class Table
    {
        public static final String USER_PROFILE = "user_profile";

        public static final String CREDENTIAL_PROFILE = "credential_profile";

        public static final String DISCOVERY_PROFILE = "discovery_profile";

        public static final String DISCOVERY_CREDENTIAL = "discovery_credential";

        public static final String DISCOVERY_RESULT = "discovery_result";

        public static final String MONITOR = "monitor";

        public static final String METRIC = "metric";

        public static final String POLLING_RESULT = "polling_result";
    }

    public static class CreateSchemaQueries
    {
        public static final String USER_PROFILE = """
            CREATE TABLE IF NOT EXISTS user_profile (
                id SERIAL PRIMARY KEY,
                username VARCHAR(255) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL
            );
            """;

        public static final String CREDENTIAL_PROFILE = """
            CREATE TABLE IF NOT EXISTS credential_profile (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) UNIQUE NOT NULL,
                device_type VARCHAR(255) DEFAULT 'windows',
                credential JSONB NOT NULL
            );
            """;

        public static final String DISCOVERY_PROFILE = """
            CREATE TABLE IF NOT EXISTS discovery_profile (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) UNIQUE NOT NULL,
                ip VARCHAR(255) NOT NULL,
                ip_type VARCHAR(50) NOT NULL,
                status VARCHAR(50) DEFAULT 'PENDING',
                port INTEGER
            );
            """;

        public static final String DISCOVERY_CREDENTIAL = """
            CREATE TABLE IF NOT EXISTS discovery_credential (
                discovery_profile INTEGER REFERENCES discovery_profile(id) ON DELETE CASCADE,
                credential_profile INTEGER REFERENCES credential_profile(id) ON DELETE CASCADE,
                PRIMARY KEY (discovery_profile, credential_profile)
            );
        """;

        public static final String DISCOVERY_RESULT = """
            CREATE TABLE IF NOT EXISTS discovery_result (
                discovery_profile INTEGER REFERENCES discovery_profile(id) ON DELETE CASCADE,
                credential_profile INTEGER REFERENCES credential_profile(id) ON DELETE RESTRICT,
                ip VARCHAR(255) NOT NULL,
                message TEXT,
                status VARCHAR(50) NOT NULL,
                time VARCHAR(50),
                PRIMARY KEY (discovery_profile, ip)
            );
        """;

        public static final String MONITOR = """
            CREATE TABLE IF NOT EXISTS monitor (
                id SERIAL PRIMARY KEY,
                ip VARCHAR(255) UNIQUE NOT NULL,
                port INTEGER,
                credential_profile INTEGER REFERENCES credential_profile(id) ON DELETE RESTRICT
            );
        """;

        public static final String METRIC = """
             CREATE TABLE IF NOT EXISTS metric (
                id SERIAL PRIMARY KEY,
                monitor INTEGER REFERENCES monitor(id) ON DELETE CASCADE,
                name VARCHAR(50) NOT NULL,
                polling_interval INTEGER NOT NULL,
                is_enabled BOOLEAN NOT NULL DEFAULT true,
                UNIQUE (monitor, name)
            );
        """;

        public static final String POLLING_RESULT = """
            CREATE TABLE IF NOT EXISTS polling_result (
                id SERIAL PRIMARY KEY,
                monitor INTEGER REFERENCES monitor(id) ON DELETE CASCADE,
                name VARCHAR(50) NOT NULL,
                data JSONB NOT NULL,
                time VARCHAR(50)
            );
        """;

    }

    public static class User
    {
        public static final String ID = "id";

        public static final String USERNAME = "username";

        public static final String PASSWORD = "password";

        public static final String JWT_KEY = "jwt";
    }

    public static class Credential
    {
        public static final String ID = "id";

        public static final String NAME = "name";

        public static final String DEVICE_TYPE = "device_type";

        public static final String CREDENTIAL = "credential";
    }

    public static class Discovery
    {
        public static final String ID = "id";

        public static final String NAME = "name";

        public static final String IP = "ip";

        public static final String IP_TYPE = "ip_type";

        public static final String STATUS = "status";

        public static final String PENDING_STATUS = "PENDING";

        public static final String RUNNING_STATUS = "RUNNING";

        public static final String COMPLETED_STATUS = "COMPLETED";

        public static final String PORT = "port";

    }

    public static class DiscoveryCredential
    {
        public static final String DISCOVERY_PROFILE = "discovery_profile";

        public static final String CREDENTIAL_PROFILE = "credential_profile";
    }

    public static class DiscoveryResult
    {
        public static final String DISCOVERY_PROFILE = "discovery_profile";

        public static final String CREDENTIAL_PROFILE = "credential_profile";

        public static final String FAILED_STATUS = "FAILED";

        public static final String COMPLETED_STATUS = "COMPLETED";

        public static final String MESSAGE = "message";

        public static final String TIME = "time";
    }

    public static class Monitor
    {
        public static final String ID = "id";

        public static final String IP = "ip";

        public static final String PORT = "port";

        public static final String CREDENTIAL = "credential";
    }

    public static class Metric
    {
        public static final String ID = "id";

        public static final String NAME = "name";

        public static final String POLLING_INTERVAL = "polling_interval";

        public static final String IS_ENABLED = "is_enabled";

    }
}
