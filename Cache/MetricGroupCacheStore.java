package org.nms.Cache;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nms.App;
import org.nms.ConsoleLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MetricGroupCacheStore {
    private static final ConcurrentHashMap<Integer, JsonObject> cachedMetricGroups = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, JsonObject> referencedMetricGroups = new ConcurrentHashMap<>();
    private static final String POLLING_INTERVAL_KEY = "polling_interval";

    public static void setCachedMetricGroup(Integer key, JsonObject value) {
        ConsoleLogger.debug("Inserting " + key + " Value: " + value);
        cachedMetricGroups.put(key, value);
    }

    public static Future<JsonArray> populate()
    {

        return App.provisionService
                .getAll()
                .onSuccess(provisionedIps -> {

                    if(provisionedIps.isEmpty()){
                        ConsoleLogger.info("There is no provisions to cache");
                    }

                    ConsoleLogger.debug(provisionedIps.encode());
                    // INSERT Into Cache

                    for(var i = 0; i < provisionedIps.size(); i++) {
                        var provisionedObject = provisionedIps.getJsonObject(i);

                        ConsoleLogger.debug("Inside DEBUG");

                        for(var k = 0; k < provisionedObject.getJsonArray("metric_groups").size(); k++) {
                            // Create a NEW JsonObject for each metric group
                            var metricGroupValue = new JsonObject()
                                    .put("provision_profile_id", provisionedObject.getInteger("id"))
                                    .put("port", Integer.parseInt(provisionedObject.getString("port")))
                                    // Make a copy of the credential object to avoid reference issues
                                    .put("credential", provisionedObject.getJsonObject("credential").copy())
                                    .put("ip", provisionedObject.getString("ip"))
                                    .put("name", provisionedObject.getJsonArray("metric_groups").getJsonObject(k).getString("name"))
                                    .put("polling_interval", provisionedObject.getJsonArray("metric_groups").getJsonObject(k).getInteger("polling_interval"));

                            var key = provisionedObject.getJsonArray("metric_groups").getJsonObject(k).getInteger("id");
                            ConsoleLogger.debug("Adding metric group with ID: " + key + " and name: " +
                                    provisionedObject.getJsonArray("metric_groups").getJsonObject(k).getString("name"));

                            MetricGroupCacheStore.setCachedMetricGroup(key, metricGroupValue);
                            MetricGroupCacheStore.setReferencedMetricGroup(key, metricGroupValue);
                        }
                    }
                    ConsoleLogger.info("Cache populated " + provisionedIps.size());

                });

    }
    public static void setReferencedMetricGroup(Integer key, JsonObject value) {
        referencedMetricGroups.put(key, value);
    }

    public static void clear() {
        cachedMetricGroups.clear();
        referencedMetricGroups.clear();
    }


    /**
     * Gets metric groups that have timed out and need polling.
     * Returns the timed-out metric groups and resets their polling intervals
     * from reference values.
     */
    public static List<JsonObject> getTimedOutMetricGroups() {
        List<JsonObject> timedOutMetricGroups = new ArrayList<>();

        cachedMetricGroups.forEach((key, value) -> {
            Integer pollingInterval = value.getInteger(POLLING_INTERVAL_KEY, 0);
            if (pollingInterval <= 0) {
                // Add a copy to the timed-out list
                synchronized (value) { // Ensure thread-safe access to JsonObject
                    timedOutMetricGroups.add(value.copy());
                }

                // Reset polling interval from reference
                JsonObject reference = referencedMetricGroups.get(key);
                if (reference != null) {
                    Integer newInterval = reference.getInteger(POLLING_INTERVAL_KEY);
                    if (newInterval != null && newInterval > 0) {
                        // Update interval in a thread-safe manner
                        JsonObject updated = value.copy();
                        updated.put(POLLING_INTERVAL_KEY, newInterval);
                        cachedMetricGroups.put(key, updated);
                    } else {
                        ConsoleLogger.warn("Invalid or missing polling_interval in reference for key: " + key);
                    }
                } else {
                    ConsoleLogger.warn("No reference found for key: " + key + ". Polling interval not reset.");
                }
            }
        });

        ConsoleLogger.debug("Found " + timedOutMetricGroups.size() + " timed-out metric groups");

        return timedOutMetricGroups;
    }

    public static void decrementMetricGroupInterval(int interval) {

        cachedMetricGroups.forEach((key, value) -> {
            synchronized (value) { // Ensure thread-safe access to JsonObject
                Integer currentInterval = value.getInteger(POLLING_INTERVAL_KEY, 0);
                JsonObject updated = value.copy();
                updated.put(POLLING_INTERVAL_KEY, Math.max(0, currentInterval - interval));
                cachedMetricGroups.put(key, updated);
            }
        });
        ;
    }
}