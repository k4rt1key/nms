package org.nms.Cache;

import io.vertx.core.json.JsonObject;
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

    public static void setReferencedMetricGroup(Integer key, JsonObject value) {
        referencedMetricGroups.put(key, value);
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
        for (JsonObject t : timedOutMetricGroups) {
            ConsoleLogger.debug("Timed-out metric group: " + t);
        }
        return timedOutMetricGroups;
    }

    public static void decrementMetricGroupInterval(int interval) {
        ConsoleLogger.debug("Decrementing polling intervals by " + interval + " seconds");

        cachedMetricGroups.forEach((key, value) -> {
            synchronized (value) { // Ensure thread-safe access to JsonObject
                Integer currentInterval = value.getInteger(POLLING_INTERVAL_KEY, 0);
                JsonObject updated = value.copy();
                updated.put(POLLING_INTERVAL_KEY, Math.max(0, currentInterval - interval));
                cachedMetricGroups.put(key, updated);
            }
        });

        cachedMetricGroups.forEach((key, value) ->
                ConsoleLogger.debug("Value after decrement for key " + key + ": " + value)
        );
    }
}