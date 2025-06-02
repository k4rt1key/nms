package org.nms.constants;

public class DatabaseConstant
{
    public static final String QUERY = "query";

    public static final String MODE = "query.mode";

    public static final String MODE_WITHOUT_DATA = "mode.without.data";

    public static final String MODE_SINGLE = "mode.single";

    public static final String MODE_BATCH = "mode.batch";

    public static final String DATA = "data";

    public static final String BATCH_DATA = "batch.data";

    public static class SchemaName
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

    public static class Status
    {
        public static final String PENDING_STATUS = "PENDING";

        public static final String RUNNING_STATUS = "RUNNING";

        public static final String COMPLETED_STATUS = "COMPLETED";

        public static final String SUCCESS = "success";

        public static final String FAILED = "failed";
    }

    public static class UserProfileSchema
    {
        public static final String ID = "id";

        public static final String USERNAME = "username";

        public static final String PASSWORD = "password";
    }

    public static class CredentialProfileSchema
    {
        public static final String ID = "id";

        public static final String NAME = "name";

        public static final String DEVICE_TYPE = "device_type";

        public static final String CREDENTIAL = "credential";
    }

    public static class DiscoveryProfileSchema
    {
        public static final String ID = "id";

        public static final String NAME = "name";

        public static final String IP = "ip";

        public static final String IP_TYPE = "ip_type";

        public static final String STATUS = "status";

        public static final String PORT = "port";

    }

    public static class DiscoveryCredentialSchema
    {
        public static final String DISCOVERY_PROFILE = "discovery_profile";

        public static final String CREDENTIAL_PROFILE = "credential_profile";
    }

    public static class DiscoveryResultSchema
    {
        public static final String DISCOVERY_PROFILE = "discovery_profile";

        public static final String CREDENTIAL_PROFILE = "credential_profile";

        public static final String IP = "ip";

        public static final String STATUS = "status";

        public static final String MESSAGE = "message";

        public static final String TIME = "time";
    }

    public static class MonitorSchema
    {
        public static final String ID = "id";

        public static final String IP = "ip";

        public static final String PORT = "port";

        public static final String CREDENTIAL = "credential";
    }

    public static class MetricSchema
    {
        public static final String ID = "id";

        public static final String MONITOR = "monitor";

        public static final String NAME = "name";

        public static final String POLLING_INTERVAL = "polling_interval";

        public static final String IS_ENABLED = "is_enabled";
    }

    public static class Operation
    {
        public static final String GET = "get";

        public static final String GET_BY_USERNAME = "get.by.username";

        public static final String LIST = "list";

        public static final String INSERT = "insert";

        public static final String UPDATE = "update";

        public static final String DELETE = "delete";

        public static final String CREATE_SCHEMA = "create.schema";

    }
}
