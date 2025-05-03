package org.nms.Scheduler;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;
import org.nms.Cache.MetricGroupCacheStore;
import org.nms.ConsoleLogger;
import org.nms.Database.Services.PolledDataService;
import org.nms.PluginManager.PluginManager;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Scheduler {
    private final Vertx vertx;
    private final long timerIntervalMs;
    private long timerId;
    private final PolledDataService polledDataService;

    /**
     * Constructor for Scheduler
     * @param vertx The Vertx instance to use for scheduling
     * @param intervalSeconds The interval at which the scheduler runs in seconds
     */
    public Scheduler(Vertx vertx, int intervalSeconds) {
        this.vertx = vertx;
        this.timerIntervalMs = TimeUnit.SECONDS.toMillis(intervalSeconds);
        this.polledDataService = PolledDataService.getInstance();
    }

    /**
     * Starts the scheduler
     */
    public void start() {
        ConsoleLogger.debug("Starting scheduler with interval: " + timerIntervalMs + "ms");
        this.timerId = vertx.setPeriodic(timerIntervalMs, id -> processMetricGroups());
    }

    /**
     * Stops the scheduler
     */
    public void stop() {
        ConsoleLogger.debug("Stopping scheduler");
        if (timerId != 0) {
            vertx.cancelTimer(timerId);
            timerId = 0;
        }
    }

    /**
     * Process metric groups, decrementing intervals and handling timed-out groups
     */
    private void processMetricGroups() {
        ConsoleLogger.debug("Processing metric groups");

        // Calculate time to decrement (based on scheduler interval)
        int decrementIntervalSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(timerIntervalMs);

        // Decrement intervals in cache
        MetricGroupCacheStore.decrementMetricGroupInterval(decrementIntervalSeconds);

        // Get timed-out metric groups
        List<JsonObject> timedOutGroups = MetricGroupCacheStore.getTimedOutMetricGroups();

        if (!timedOutGroups.isEmpty()) {
            ConsoleLogger.debug("Found " + timedOutGroups.size() + " timed-out metric groups");

            // Convert timed-out groups to expected format
            JsonObject requestPayload = preparePollingRequest(timedOutGroups);

            // Send to plugin manager
            sendToPluginManager(requestPayload)
                    .onSuccess(this::processAndSaveResults)
                    .onFailure(err -> ConsoleLogger.error("Error in plugin manager polling: " + err.getMessage()));
        } else {
            ConsoleLogger.debug("No timed-out metric groups found");
        }
    }

    /**
     * Prepare the polling request payload in the required format
     * @param timedOutGroups List of timed-out metric groups
     * @return JsonObject with the formatted request
     */
    private JsonObject preparePollingRequest(List<JsonObject> timedOutGroups) {
        JsonArray metricGroups = new JsonArray();

        for (JsonObject metricGroup : timedOutGroups) {
            JsonObject groupData = new JsonObject()
                    .put("provision_profile_id", metricGroup.getInteger("provision_profile_id"))
                    .put("name", metricGroup.getString("name"))
                    .put("ip", metricGroup.getString("ip"))
                    .put("port", metricGroup.getInteger("port"))
                    .put("credential", metricGroup.getJsonObject("credential"));

            metricGroups.add(groupData);
        }

        return new JsonObject()
                .put("type", "polling")
                .put("metric_groups", metricGroups);
    }

    /**
     * Send the request to plugin manager
     * @param requestPayload The request payload
     * @return Future with the response from the plugin manager
     */
    private Future<JsonArray> sendToPluginManager(JsonObject requestPayload) {
        Promise<JsonArray> promise = Promise.promise();

        ConsoleLogger.debug("Request payload " + requestPayload);

        ConsoleLogger.debug("Sending polling request to plugin manager: " + requestPayload.encode());

        // Extract required parameters for plugin manager
        JsonArray ips = new JsonArray();
        int port = 0;
        JsonArray credentials = new JsonArray();

        // Extract IPs, port, and credentials from the metric groups
        JsonArray metricGroups = requestPayload.getJsonArray("metric_groups");
        for (int i = 0; i < metricGroups.size(); i++) {
            JsonObject metricGroup = metricGroups.getJsonObject(i);
            ips.add(metricGroup.getString("ip"));

            // Use the first port found (assuming all are the same)
            if (port == 0) {
                port = metricGroup.getInteger("port");
            }

            // Add credential if not already present
            JsonObject credential = metricGroup.getJsonObject("credential");
            boolean credentialExists = false;

            for (int j = 0; j < credentials.size(); j++) {
                if (credential.getInteger("id").equals(credential.getInteger("id"))) {
                    credentialExists = true;
                    break;
                }
            }

            if (!credentialExists) {
                credentials.add(credential);
            }
        }

        // Generate a unique discovery ID (using timestamp)
        int discoveryId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);

        // Send to plugin manager
        PluginManager.runDiscovery(discoveryId, ips, port, credentials)
                .onSuccess(promise::complete)
                .onFailure(promise::fail);

        return promise.future();
    }

    /**
     * Process and save the results from plugin manager
     * @param results The results from plugin manager
     */
    private void processAndSaveResults(JsonArray results) {
        ConsoleLogger.debug("Processing results from plugin manager: " + results.encode());

        // Create a batch of parameters for saving to database
        JsonArray batchParams = new JsonArray();

        for (int i = 0; i < results.size(); i++) {
            JsonObject result = results.getJsonObject(i);

            // Skip unsuccessful results
            if (!result.getBoolean("success", false)) {
                ConsoleLogger.warn("Skipping unsuccessful result: " + result.encode());
                continue;
            }

            // Add timestamp if not present
            if (result.getString("time", "").isEmpty()) {
                result.put("time", ZonedDateTime.now().toString());
            }

            // Create parameters for database insert

            batchParams.add(Tuple.wrap(List.of(
                    result.getInteger("provision_profile_id"),
                    result.getString("name"),
                    result.getString("data"))));
        }

        // Save to database
        if (!batchParams.isEmpty()) {
            polledDataService.save(batchParams)
                    .onSuccess(saved -> ConsoleLogger.debug("Saved " + saved.size() + " polling results to database"))
                    .onFailure(err -> ConsoleLogger.error("Error saving polling results: " + err.getMessage()));
        }
    }
}