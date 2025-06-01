package org.nms.constants;

public class Plugin
{
    public static class DiscoveryRequest
    {
        public static final String TYPE = "type";

        public static final String DISCOVERY = "discovery";

        public static final String ID = "id";

        public static final String PORT = "port";

        public static final String IPS = "ips";

        public static final String CREDENTIALS = "credentials";
    }

    public static class DiscoveryResponse
    {
        public static final String ID = "id";

        public static final String IP = "ip";

        public static final String CREDENTIALS = "credential";
    }

    public static class PollingRequest
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

    public static class PollingResponse
    {
        public static final String MONITOR_ID = "monitor_id";

        public static final String DATA = "data";

        public static final String NAME = "name";

        public static final String TIME = "time";
    }
}
