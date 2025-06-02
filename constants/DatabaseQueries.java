package org.nms.constants;

public class DatabaseQueries
{
    public static class UserProfile
    {
        public static final String CREATE_SCHEMA = """
            CREATE TABLE IF NOT EXISTS user_profile (
                id SERIAL PRIMARY KEY,
                username VARCHAR(255) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL
            );
            """;

        public static final String GET = """
            SELECT * FROM user_profile WHERE id = $1;
            """;

        public static final String GET_BY_USERNAME = """
            SELECT * FROM user_profile WHERE username = $1;
            """;

        public static final String LIST = """
            SELECT * FROM user_profile;
            """;

        public static final String INSERT = """
            INSERT INTO user_profile (username, password) VALUES ($1, $2) RETURNING *;
            """;

        public static final String UPDATE = """
            UPDATE user_profile
            SET
                username = COALESCE($2, username),
                password = COALESCE($3, password)
            WHERE id = $1
            RETURNING *;
            """;

        public static final String DELETE = """
            DELETE FROM user_profile WHERE id = $1 RETURNING *;
            """;
    }

    public static class CredentialProfile
    {
        public static final String CREATE_SCHEMA = """
            CREATE TABLE IF NOT EXISTS credential_profile (
                id SERIAL PRIMARY KEY,
                device_type VARCHAR(255) DEFAULT 'windows',
                name VARCHAR(255) UNIQUE NOT NULL,
                credential JSONB NOT NULL
            );
            """;

        public static final String GET = """
            SELECT * FROM credential_profile WHERE id = $1;
            """;

        public static final String LIST = """
            SELECT * FROM credential_profile;
            """;

        public static final String INSERT = """
            INSERT INTO credential_profile (name, credential, device_type) VALUES ($1, $2, $3) RETURNING *;
            """;

        public static final String UPDATE = """
            UPDATE credential_profile
            SET
                name = COALESCE($2, name),
                credential = COALESCE($3, credential),
                device_type = COALESCE($4, device_type)
            WHERE id = $1
            RETURNING *;
            """;

        public static final String DELETE = """
            DELETE FROM credential_profile WHERE id = $1 RETURNING *;
            """;
    }

    public static class DiscoveryProfile
    {
        public static final String CREATE_SCHEMA = """
            CREATE TABLE IF NOT EXISTS discovery_profile (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) UNIQUE NOT NULL,
                ip VARCHAR(255) NOT NULL,
                ip_type VARCHAR(50) NOT NULL,
                status VARCHAR(50) DEFAULT 'PENDING',
                port INTEGER
            );
            """;

        public static final String GET = """
           SELECT
                dp.id AS id,
                dp.name AS name,
                dp.ip AS ip,
                dp.ip_type AS ip_type,
                dp.status AS status,
                dp.port AS port,
                ARRAY_AGG(
                    JSON_BUILD_OBJECT(
                        'id', cp.id,
                        'name', cp.name,
                        'device_type', cp.device_type,
                        'credential', cp.credential
                    )
                ) AS credential_profile
            FROM discovery_profile dp
            LEFT JOIN discovery_credential dc ON dp.id = dc.discovery_profile
            LEFT JOIN credential_profile cp ON dc.credential_profile = cp.id
            WHERE dp.id = $1
            GROUP BY dp.id, dp.name, dp.ip, dp.ip_type, dp.status, dp.port;
           """;

        public static final String LIST = """
              SELECT
                dp.id AS id,
                dp.name AS name,
                dp.ip AS ip,
                dp.ip_type AS ip_type,
                dp.status AS status,
                dp.port AS port,
                ARRAY_AGG(
                    JSON_BUILD_OBJECT(
                        'id', cp.id,
                        'name', cp.name,
                        'device_type', cp.device_type,
                        'credential', cp.credential
                    )
                ) AS credential_profile
            FROM discovery_profile dp
            LEFT JOIN discovery_credential dc ON dp.id = dc.discovery_profile
            LEFT JOIN credential_profile cp ON dc.credential_profile = cp.id
            GROUP BY dp.id, dp.name, dp.ip, dp.ip_type, dp.status, dp.port;
            """;

        public static final String INSERT = """
            INSERT INTO discovery_profile (name, ip, ip_type, status, port) VALUES ($1, $2, $3, 'PENDING', $4) RETURNING *;
            """;


        public static final String UPDATE = """
            UPDATE discovery_profile
            SET
                name = COALESCE($2, name),
                ip = COALESCE($3, ip),
                ip_type = COALESCE($4, ip_type),
                port = COALESCE($5, port),
                status = COALESCE($6, status)
            WHERE id = $1
            RETURNING *;
            """;

        public static final String DELETE = """
            DELETE FROM discovery_profile WHERE id = $1 RETURNING *;
            """;
    }

    public static class DiscoveryCredential
    {
        public static final String CREATE_SCHEMA = """
            CREATE TABLE IF NOT EXISTS discovery_credential (
                discovery_profile INTEGER REFERENCES discovery_profile(id) ON DELETE CASCADE,
                credential_profile INTEGER REFERENCES credential_profile(id) ON DELETE CASCADE,
                PRIMARY KEY (discovery_profile, credential_profile)
            );
            """;

        public static final String INSERT = """
            INSERT INTO discovery_credential (discovery_profile, credential_profile)
            VALUES ($1, $2)
            ON CONFLICT (discovery_profile, credential_profile) DO NOTHING
            RETURNING *;
            """;

        public static final String DELETE = """
            DELETE FROM discovery_credential dc
            USING discovery_profile dp
            WHERE dc.discovery_profile = dp.id
              AND dp.status = 'PENDING'
              AND dc.discovery_profile = $1
              AND dc.credential_profile = $2
            RETURNING *;
            """;
    }

    public static class DiscoveryResult
    {
        public static final String CREATE_SCHEMA = """
            CREATE TABLE IF NOT EXISTS discovery_result (
                discovery_profile INTEGER REFERENCES discovery_profile(id) ON DELETE CASCADE,
                credential_profile INTEGER REFERENCES credential_profile(id) ON DELETE RESTRICT,
                ip VARCHAR(255) NOT NULL,
                message TEXT,
                status VARCHAR(50) NOT NULL,
                PRIMARY KEY (discovery_profile, ip),
                time VARCHAR(50)
            );
            """;

        public static final String GET = """
            SELECT
                dr.discovery_profile,
                dr.ip,
                dr.message,
                dr.status,
                dr.time,
                json_build_object(
                    'id', cp.id,
                    'name', cp.name,
                    'device_type', cp.device_type,
                    'credential', cp.credential
                ) AS credential_profile
            FROM discovery_result dr
            JOIN credential_profile cp ON dr.credential_profile = cp.id
            WHERE dr.discovery_profile = $1;
            """;

        public static final String INSERT = """
            INSERT INTO discovery_result (
                discovery_profile,
                credential_profile,
                ip,
                message,
                status,
                time
            ) VALUES ($1, $2, $3, $4, $5, $6)
            ON CONFLICT (discovery_profile, ip) DO UPDATE
            SET
                credential_profile = EXCLUDED.credential_profile,
                message = EXCLUDED.message,
                status = EXCLUDED.status,
                time = EXCLUDED.time
            RETURNING *;
            """;

        public static final String DELETE = """
            DELETE FROM discovery_result WHERE discovery_profile = $1;
            """;

    }

    public static class Monitor
    {
        public static final String CREATE_SCHEMA = """
            CREATE TABLE IF NOT EXISTS monitor (
                id SERIAL PRIMARY KEY,
                ip VARCHAR(255) UNIQUE NOT NULL,
                port INTEGER,
                credential_profile INTEGER REFERENCES credential_profile(id) ON DELETE RESTRICT
            );
            """;

        public static final String GET = """
            SELECT p.id, p.ip, p.port,
                   json_build_object(
                       'id', c.id,
                       'name', c.name,
                       'device_type', c.device_type,
                       'credential', c.credential
                   ) AS credential_profile,
                   json_agg(json_build_object(
                       'id', m.id,
                       'monitor', m.monitor,
                       'name', m.name,
                       'polling_interval', m.polling_interval,
                       'isEnabled', m.isEnabled
                   )) AS metric
            FROM monitor p
            LEFT JOIN credential_profile c ON p.credential_profile = c.id
            LEFT JOIN metric m ON p.id = m.monitor
            WHERE p.id = $1
            GROUP BY p.id, c.id;
            """;

        public static final String LIST = """
                 SELECT p.id, p.ip, p.port,
                   json_build_object(
                       'id', c.id,
                       'name', c.name,
                       'device_type', c.device_type,
                       'credential', c.credential
                   ) AS credential_profile,
                   json_agg(json_build_object(
                       'id', m.id,
                       'monitor', m.monitor,
                       'name', m.name,
                       'polling_interval', m.polling_interval,
                       'isEnabled', m.isEnabled
                   )) AS metric
            FROM monitor p
            LEFT JOIN credential_profile c ON p.credential_profile = c.id
            LEFT JOIN metric m ON p.id = m.monitor
            GROUP BY p.id, c.id;
            """;

        public static final String INSERT = """
            WITH discovery_validation AS (
                SELECT dr.ip, dr.credential_profile, dp.port
                FROM discovery_result dr
                JOIN discovery_profile dp ON dr.discovery_profile = dp.id
                WHERE dr.discovery_profile = $1
                AND dr.ip = $2
                AND dr.status = 'COMPLETED'
            ),
            inserted_monitor AS (
                INSERT INTO monitor (ip, port, credential_profile)
                SELECT ip, port, credential_profile
                FROM discovery_validation
                RETURNING *
            ),
            metric_names AS (
                SELECT 'CPU' UNION ALL
                SELECT 'UPTIME' UNION ALL
                SELECT 'MEMORY' UNION ALL
                SELECT 'DISK' UNION ALL
                SELECT 'PROCESS' UNION ALL
                SELECT 'NETWORK' UNION ALL
                SELECT 'SYSTEMINFO'
            ),
            inserted_metrics AS (
                INSERT INTO metric (monitor, name, polling_interval)
                SELECT p.id, m.name,"""+ Configuration.SCHEDULER_CHECKING_INTERVAL + """
                FROM inserted_monitor p
                CROSS JOIN metric_names m
                RETURNING *
            )
            SELECT
                p.*,
                json_build_object(
                       'id', c.id,
                       'name', c.name,
                       'device_type', c.device_type,
                       'credential', c.credential
                ) AS credential_profile,
                COALESCE(
                    (SELECT json_agg(
                        json_build_object(
                            'id', m.id,
                            'monitor', m.monitor,
                            'name', m.name,
                            'polling_interval', m.polling_interval,
                            'isEnabled', m.isEnabled
                        )
                    )
                    FROM inserted_metrics m
                    WHERE m.monitor = p.id),
                    '[]'::json
                ) AS metric
            FROM inserted_monitor p
            JOIN credential_profile c ON p.credential_profile = c.id;
            """;

        public static final String DELETE = """
            DELETE FROM monitor WHERE id = $1 RETURNING *;
            """;
    }

    public static class Metric
    {
        public static final String CREATE_SCHEMA = """
            CREATE TABLE IF NOT EXISTS metric (
                id SERIAL PRIMARY KEY,
                monitor INTEGER REFERENCES monitor(id) ON DELETE CASCADE,
                name VARCHAR(50) NOT NULL,
                polling_interval INTEGER NOT NULL,
                isEnabled BOOLEAN NOT NULL DEFAULT true,
                UNIQUE (monitor, name)
            );
            """;

        public static final String UPDATE = """
            UPDATE metric
            SET
                polling_interval = COALESCE($2, polling_interval),
                isEnabled = COALESCE($4, isEnabled)
            WHERE id = $1 RETURNING *;
            """;
    }

    public static class PollingResult
    {
        public static final String CREATE_SCHEMA = """
            CREATE TABLE IF NOT EXISTS polling_result (
                id SERIAL PRIMARY KEY,
                monitor INTEGER REFERENCES monitor(id) ON DELETE CASCADE,
                name VARCHAR(50) NOT NULL,
                data JSONB NOT NULL,
                time VARCHAR(50)
            );
            """;

        public static final String LIST = """
            SELECT * FROM polling_result;
            """;

        public static final String INSERT = """
            INSERT INTO polling_result (monitor, name, data, time) VALUES ($1, $2, $3, $4) RETURNING *;
            """;
    }
}