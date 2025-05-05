package org.nms.Scheduler;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;
import org.nms.App;
import org.nms.Cache.MetricGroupCacheStore;
import org.nms.ConsoleLogger;
import org.nms.Database.Services.PolledDataService;
import org.nms.PluginManager.PluginManager;

import java.time.ZonedDateTime;
import java.util.ArrayList;
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
        MetricGroupCacheStore.populate().onSuccess((res)-> this.timerId = vertx.setPeriodic(timerIntervalMs, id -> processMetricGroups()))
                .onFailure(err -> ConsoleLogger.error("Error running scheduler " + err.getMessage()));


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

            // Prepare metric groups for polling request
            JsonArray metricGroups = preparePollingMetricGroups(timedOutGroups);

            // Send to plugin manager using the proper polling method
            PluginManager.runPolling(metricGroups)
                    .onSuccess(this::processAndSaveResults)
                    .onFailure(err -> ConsoleLogger.error("Error in plugin manager polling: " + err.getMessage()));
        } else {
            ConsoleLogger.debug("No timed-out metric groups found");
        }
    }

    /**
     * Prepare the metric groups for polling in the required format
     * @param timedOutGroups List of timed-out metric groups
     * @return JsonArray with formatted metric groups
     */
    private JsonArray preparePollingMetricGroups(List<JsonObject> timedOutGroups) {
        JsonArray metricGroups = new JsonArray();

        for (JsonObject metricGroup : timedOutGroups) {
            JsonObject groupData = new JsonObject()
                    .put("provision_profile_id", metricGroup.getInteger("provision_profile_id"))
                    .put("name", metricGroup.getString("name"))
                    .put("ip", metricGroup.getString("ip"))
                    .put("port", metricGroup.getInteger("port"))
                    .put("credentials", metricGroup.getJsonObject("credential"));

            metricGroups.add(groupData);
        }

        ConsoleLogger.debug("Prepared metric groups for polling: " + metricGroups.encode());
        return metricGroups;
    }

    /**
     * Process and save the results from plugin manager
     * @param results The results from plugin manager
     */
    private void processAndSaveResults(JsonArray results) {
        ConsoleLogger.debug("Processing results from plugin manager: " + results.encode());

        // Create a batch of parameters for saving to database
        List<Tuple> batchParams = new ArrayList<>();

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

            ConsoleLogger.debug("DURING SAVE " + result);


            // Create parameters for database insert
            batchParams.add(Tuple.of(
                    result.getInteger("provision_profile_id"),
                    result.getString("name"),
                    result.getString("data")
            ));
        }

        // Save to database
        if (!batchParams.isEmpty()) {
            polledDataService.save(batchParams)
                    .onSuccess(saved -> ConsoleLogger.debug("Saved " + saved.size() + " polling results to database"))
                    .onFailure(err -> ConsoleLogger.error("Error saving polling results: " + err.getMessage()));
        }
    }
}