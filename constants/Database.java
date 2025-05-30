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

        public static final String CREATE_ALL_SCHEMAS = """
            BEGIN;
        
            CREATE TABLE IF NOT EXISTS app_user (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL
            );
        
            CREATE TABLE IF NOT EXISTS credential (
                id SERIAL PRIMARY KEY,
                protocol VARCHAR(255) DEFAULT 'winrm',
                name VARCHAR(255) UNIQUE NOT NULL,
                credential JSONB NOT NULL
            );
        
            CREATE TABLE IF NOT EXISTS discovery (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) UNIQUE NOT NULL,
                ip VARCHAR(255) NOT NULL,
                ip_type VARCHAR(50) NOT NULL,
                status VARCHAR(50) DEFAULT 'PENDING',
                port INTEGER
            );
        
            CREATE TABLE IF NOT EXISTS discovery_credential (
                discovery_id INTEGER REFERENCES discovery(id) ON DELETE CASCADE,
                credential_id INTEGER REFERENCES credential(id) ON DELETE CASCADE,
                PRIMARY KEY (discovery_id, credential_id)
            );
        
            CREATE TABLE IF NOT EXISTS discovery_result (
                discovery_id INTEGER REFERENCES discovery(id) ON DELETE CASCADE,
                credential_id INTEGER REFERENCES credential(id) ON DELETE RESTRICT,
                ip VARCHAR(255) NOT NULL,
                message TEXT,
                status VARCHAR(50) NOT NULL,
                time VARCHAR(50),
                PRIMARY KEY (discovery_id, ip)
            );
        
            CREATE TABLE IF NOT EXISTS monitor (
                id SERIAL PRIMARY KEY,
                ip VARCHAR(255) UNIQUE NOT NULL,
                port INTEGER,
                credential_id INTEGER REFERENCES credential(id) ON DELETE RESTRICT
            );
        
            CREATE TABLE IF NOT EXISTS metric_group (
                id SERIAL PRIMARY KEY,
                monitor_id INTEGER REFERENCES monitor(id) ON DELETE CASCADE,
                name VARCHAR(50) NOT NULL,
                polling_interval INTEGER NOT NULL,
                isEnabled BOOLEAN NOT NULL DEFAULT true,
                UNIQUE (monitor_id, name)
            );
        
            CREATE TABLE IF NOT EXISTS polling_result (
                id SERIAL PRIMARY KEY,
                monitor_id INTEGER REFERENCES monitor(id) ON DELETE CASCADE,
                name VARCHAR(50) NOT NULL,
                data JSONB NOT NULL,
                time VARCHAR(50)
            );
        
            COMMIT;
        """;

    }

    public static class Operation
    {
        public static final String LIST = "list";

        public static final String GET = "get";

        public static final String INSERT = "insert";

        public static final String UPDATE = "update";

        public static final String DELETE = "delete";
    }

    public static class Table
    {
        public static final String USER = "users";

        public static final String CREDENTIAL = "credential";

        public static final String DISCOVERY = "discovery";

        public static final String DISCOVERY_CREDENTIAL = "discovery_credential";

        public static final String DISCOVERY_RESULT = "discovery_result";

        public static final String MONITOR = "monitor";

        public static final String METRIC = "metric";

        public static final String POLL_RESULTS = "poll_results";
    }
}
