package org.nms.cache;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.nms.ConsoleLogger;
import org.nms.constants.Fields;
import org.nms.constants.Fields.*;
import org.nms.constants.Queries;
import org.nms.database.DbEngine;

public class MonitorCache
{
    private static final ConcurrentHashMap<Integer, JsonObject> cachedMetricGroups = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Integer, JsonObject> referencedMetricGroups = new ConcurrentHashMap<>();

    // ===== Populates Cache From DB =====
    public static Future<JsonArray> populate()
    {

        try {
            return DbEngine.execute(Queries.Monitor.GET_ALL)
                    .onSuccess(monitorArray ->
                    {
                        if (monitorArray.isEmpty()) {
                            return;
                        }

                        insertMonitorArray(monitorArray);
                    });
        }
        catch (Exception e)
        {
            ConsoleLogger.error("Error Populating Cache");

            return Future.failedFuture("Error Populating Cache");
        }
    }

    // ===== Adds monitored Objects into cache =====
    public static void insertMonitorArray(JsonArray monitorArray)
    {
        // Step-1 : Iterate Over All monitors
        for(var i = 0; i < monitorArray.size(); i++)
        {
            var monitorObject = monitorArray.getJsonObject(i);

            // Step-2 : Iterate Over All Metric Group Of Particular monitor
            for(var k = 0; k < monitorObject.getJsonArray(Monitor.METRIC_GROUP_JSON).size(); k++)
            {
                try {
                    var metricObject = monitorObject.getJsonArray(Monitor.METRIC_GROUP_JSON).getJsonObject(k);

                    var value = new JsonObject()
                            .put(Fields.MonitorCache.ID, metricObject.getInteger(Fields.MetricGroup.ID))
                            .put(Fields.MonitorCache.MONITOR_ID, monitorObject.getInteger(Fields.Monitor.ID))
                            .put(Fields.MonitorCache.PORT, Integer.valueOf(monitorObject.getString(Monitor.PORT)))
                            .put(Fields.MonitorCache.CREDENTIALS, monitorObject.getJsonObject(Monitor.CREDENTIAL_JSON).copy())
                            .put(Fields.MonitorCache.IP, monitorObject.getString(Fields.Monitor.IP))
                            .put(Fields.MonitorCache.NAME, metricObject.getString(Fields.MetricGroup.NAME))
                            .put(Fields.MonitorCache.POLLING_INTERVAL, metricObject.getInteger(Fields.MetricGroup.POLLING_INTERVAL))
                            .put(Fields.MonitorCache.IS_ENABLED, metricObject.getBoolean(Fields.MetricGroup.IS_ENABLED));

                    var key = monitorObject.getJsonArray(Monitor.METRIC_GROUP_JSON).getJsonObject(k).getInteger(Fields.MetricGroup.ID);

                    referencedMetricGroups.put(key, value.copy());

                    cachedMetricGroups.put(key, value.copy());
                }
                catch (Exception e)
                {
                    ConsoleLogger.warn("Error Inserting Some Monitor in cache");
                }
            }
        }

        ConsoleLogger.info("\uD83D\uDCE9 Inserted " + monitorArray.size() + " monitors Into Cache, Now Total Number Of Entry In Cache Is " + cachedMetricGroups.size());
    }

    // ===== Updates MetricGroup present into cache =====
    public static void updateMetricGroups(JsonArray metricGroups)
    {

        for(var i = 0; i < metricGroups.size(); i++)
        {
            try {
                var metricGroup = metricGroups.getJsonObject(i);

                var key = metricGroup.getInteger(Fields.MonitorCache.ID);

                var updatedValue = referencedMetricGroups.get(key).copy();

                if (metricGroup.getInteger(Fields.MonitorCache.POLLING_INTERVAL) != null) {
                    updatedValue.put(Fields.MonitorCache.POLLING_INTERVAL, metricGroup.getInteger(Fields.MonitorCache.POLLING_INTERVAL));
                }

                if (metricGroup.getBoolean(Fields.MonitorCache.IS_ENABLED) != null && !metricGroup.getBoolean(Fields.MonitorCache.IS_ENABLED)) {
                    referencedMetricGroups.remove(key);

                    cachedMetricGroups.remove(key);
                }

                referencedMetricGroups
                        .put(
                                key,
                                updatedValue
                        );

                cachedMetricGroups
                        .put(
                                key,
                                updatedValue
                        );
            }
            catch (Exception e)
            {
                ConsoleLogger.warn("Error Updating Some Monitor In Cache");
            }
        }

        ConsoleLogger.info("➖ Updated " + metricGroups.size() + " Entries From Cache");
    }

    // ===== Deletes MetricGroup Present Into Cache =====
    public static void deleteMetricGroups(Integer monitorId)
    {
        AtomicInteger total = new AtomicInteger();

        referencedMetricGroups.forEach((key, value)->
        {
            if(value.getInteger(Fields.MonitorCache.MONITOR_ID).equals(monitorId))
            {
                referencedMetricGroups.remove(key);

                cachedMetricGroups.remove(key);

                total.incrementAndGet();
            }
        });

        ConsoleLogger.info("➖ Removed " + total.get() + " Entries From Cache");
    }

    /**
     * Gets Metric Groups That Have Timed Out And Need Polling
     * Returns Timed-Out Metric Groups And Resets Their Polling Intervals From Reference Values
     */
    public static List<JsonObject> getTimedOutMetricGroups()
    {
        List<JsonObject> timedOutMetricGroups = new ArrayList<>();

        // Iterate Over All Metric Groups And Decrement Interval
        cachedMetricGroups.forEach((key, value) ->
        {
            Integer pollingInterval = value.getInteger(Fields.MonitorCache.POLLING_INTERVAL, 0);

            if (pollingInterval <= 0)
            {
                // Found Timed Out Metric Group
                timedOutMetricGroups.add(value.copy());

                var updated = value.copy().put(Fields.MonitorCache.POLLING_INTERVAL, referencedMetricGroups.get(key).getInteger(Fields.MonitorCache.POLLING_INTERVAL));

                // Reset Interval
                cachedMetricGroups.put(
                        key,
                        updated.copy()
                );
            }
        });

        timedOutMetricGroups.forEach((timedOutMetricGroup ->
        {
            var key = timedOutMetricGroup.getInteger(Fields.MonitorCache.ID);

            var updated = timedOutMetricGroup.copy().put(Fields.MonitorCache.POLLING_INTERVAL, referencedMetricGroups.get(key).getInteger(Fields.MonitorCache.POLLING_INTERVAL));

            // Reset Interval
            cachedMetricGroups.put(
                    key,
                    updated.copy()
            );
        }));

        ConsoleLogger.debug("⏰ Found " + timedOutMetricGroups.size() + " Timed Out Metric Groups");

        return timedOutMetricGroups;
    }

    // ===== Decrement Interval Of All Entries =====
    public static void decrementMetricGroupInterval(int interval)
    {
        cachedMetricGroups.forEach((key, value) ->
        {
            Integer currentInterval = value.getInteger(Fields.MonitorCache.POLLING_INTERVAL, 0);

            Integer updatedInterval =  Math.max(0, currentInterval - interval);

            cachedMetricGroups.put(
                key,
                cachedMetricGroups.get(key).put(Fields.MonitorCache.POLLING_INTERVAL, updatedInterval)
            );
        });
    }

}