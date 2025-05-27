package org.nms.constants;

public class Fields
{
    public static class ENDPOINTS
    {
        public static final String CREDENTIALS_ENDPOINT = "/api/v1/credential/*";

        public static final String PROVISION_ENDPOINT = "/api/v1/provision/*";

        public static final String USER_ENDPOINT = "/api/v1/user/*";

        public static final String DISCOVERY_ENDPOINT = "/api/v1/discovery/*";

        public static final String RESULT_ENDPOINT = "/api/v1/result/*";
    }

    public static class User
    {
       public static final String ID = "id";

       public static final String NAME = "name";

       public static final String PASSWORD = "password";
    }

    public static class Credential
    {
        public static final String ID = "id";

        public static final String NAME = "name";

        public static final String USERNAME = "username";

        public static final String PASSWORD = "password";

        public static final String PROTOCOL = "protocol";

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

        public static final String FAILED_STATUS = "FAILED";

        public static final String COMPLETED_STATUS = "COMPLETED";

        public static final String PORT = "port";

        public static final String CREDENTIAL_JSON = "credential";

        public static final String RESULT_JSON = "results";

        public static final String ADD_CREDENTIALS = "add_credentials";

        public static final String REMOVE_CREDENTIALS = "remove_credentials";

        public static final String SUCCESS = "success";

        public static final String FAILURE = "failed";
    }

    public static class DiscoveryCredential
    {
        public static final String DISCOVERY_ID = "discovery_id";

        public static final String CREDENTIAL_ID = "credential_id";
    }

    public static class DiscoveryResult
    {
        public static final String IP = "ip";

        public static final String CREDENTIAL = "credential";

        public static final String STATUS = "status";

        public static final String PENDING_STATUS = "PENDING";

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

        public static final String CREDENTIAL_ID = "credential_id";

        public static final String CREDENTIAL_JSON = "credential";

        public static final String METRIC_GROUP_JSON = "metric_group";
    }

    public static class MetricGroup
    {
        public static final String ID = "id";

        public static final String NAME = "name";

        public static final String POLLING_INTERVAL = "polling_interval";

        public static final String IS_ENABLED = "is_enabled";

    }

    public static class PollingResult
    {
        public static final String ID = "id";

        public static final String MONITOR_ID = "monitor_id";

        public static final String NAME = "name";

        public static final String DATA = "data";

        public static final String TIME = "time";
    }

    public static class PluginDiscoveryRequest
    {
        public static final String TYPE = "type";

        public static final String DISCOVERY = "discovery";

        public static final String ID = "id";

        public static final String PORT = "port";

        public static final String IPS = "ips";

        public static final String CREDENTIALS = "credentials";
    }

    public static class PluginDiscoveryResponse
    {
        public static final String TYPE = "type";

        public static final String DISCOVERY = "discovery";

        public static final String RESULT_JSON = "results";

        public static final String ID = "id";

        public static final String MESSAGE = "message";

        public static final String PORT = "port";

        public static final String IP = "ip";

        public static final String CREDENTIALS = "credential";
    }

    public static class PluginPollingRequest
    {
        public static final String POLLING = "polling";

        public static final String TYPE = "type";

        public static final String METRIC_GROUPS = "metric_groups";

        public static final String MONITOR_ID = "monitor_id";

        public static final String IP = "ip";

        public static final String PORT = "port";

        public static final String NAME = "name";

        public static final String CREDENTIALS = "credential";
    }

    public static class PluginPollingResponse
    {
        public static final String MONITOR_ID = "monitor_id";

        public static final String METRIC_GROUPS = "metric_groups";

        public static final String DATA = "data";

        public static final String NAME = "name";
    }

    public static class MonitorCache
    {
        public static final String ID = "id";

        public static final String MONITOR_ID = "monitor_id";

        public static final String NAME = "name";

        public static final String IP = "ip";

        public static final String PORT = "port";

        public static final String CREDENTIAL = "credentials";

        public static final String POLLING_INTERVAL = "polling_interval";

        public static final String IS_ENABLED = "is_enabled";
    }

    public static class EventBus
    {
        public static final String PLUGIN_SPAWN_ADDRESS = "plugin.spawn";

        public static final String RUN_DISCOVERY_ADDRESS = "discovery";

        public static final String EXECUTE_SQL_QUERY_ADDRESS = "database.execute.sql";

        public static final String EXECUTE_SQL_QUERY_WITH_PARAMS_ADDRESS = "database.execute.sql.params";

        public static final String EXECUTE_SQL_QUERY_BATCH_ADDRESS = "database.execute.sql.batch";
    }
}