package org.nms.constants;

public class Config
{
    public static final Boolean PRODUCTION = false;

    public static final String KEYSTORE_PATH = "keystore.jks";

    public static final String JWT_SECRET = "secret";

    public static final String PLUGIN_PATH = "src/main/plugin/nms-plugin";

    public static final String DISCOVERY_ADDRESS = "discovery";

    public static final String POLLING_ADDRESS = "polling";

    public static final Integer INITIAL_OVERHEAD_TIME_FOR_PLUGIN = 5;

    public static final Integer DISCOVERY_TIMEOUT_PER_IP = 1;

    public static final Integer POLLING_TIMEOUT_PER_METRIC_GROUP = 15;

    public static final Integer SCHEDULER_CHECKING_INTERVAL_SECONDS = 60;
}
