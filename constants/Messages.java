package org.nms.constants;

public class Messages
{
    public static final String ERROR_ICON = "❌";

    public static final String WARNING_ICON = "⚠";

    public static final String SUCCESS_ICON = ",\uD83D\uDE80";

    public static final String STOP_ICON = "\uD83D\uDED1";

    // Verticle

    public static final String VERTICLE_DEPLOY_FAILED = ERROR_ICON + "Failed to deploy %s Verticle";

    public static final String VERTICLE_UNDEPLOYED = STOP_ICON + "%s Verticle stopped";

    public static final String FAILURE_UNDEPLOYING_VERTICLE = ERROR_ICON + "Failed to undeploy Verticle %s";

    // Database

    public static final String FAILURE_EXECUTING_QUERY = WARNING_ICON + "Failed to execute query %s with params %s, error => %s";
}
