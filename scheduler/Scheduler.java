package org.nms.scheduler;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;
import org.nms.App;
import org.nms.cache.MonitorCache;
import org.nms.ConsoleLogger;

import org.nms.constants.Fields;
import org.nms.constants.Queries;
import org.nms.database.DbEngine;
import org.nms.plugin.PluginManager;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class Scheduler extends AbstractVerticle
{
    private final int CHECKING_INTERVAL = 60;

    private long timerId;

    // ===== Starts Polling Scheduler =====
    @Override
    public void start()
    {
        ConsoleLogger.debug("✅ Starting SchedulerVerticle with CHECKING_INTERVAL => " + CHECKING_INTERVAL + " seconds, on thread [ " + Thread.currentThread().getName() + " ] ");

        // ===== Populate Cache From DB =====
        MonitorCache
                .populate()
                // ===== If Success : Start Timer =====
                .onSuccess((res)-> timerId = App.vertx.setPeriodic(CHECKING_INTERVAL * 1000, id -> processMetricGroups()))
                // ===== Else : Print Error =====
                .onFailure(err -> ConsoleLogger.error("❌ Error Running Scheduler => " + err.getMessage()));
    }

    // ===== Stops Scheduler =====
    @Override
    public void stop()
    {
        if (timerId != 0)
        {
            vertx.cancelTimer(timerId);

            ConsoleLogger.debug("\uD83D\uDED1 Scheduler Stopped");

            timerId = 0;
        }
    }

    // ====== Process metric groups, Decrementing intervals, Handling timed-out groups ======
    private void processMetricGroups()
    {
        // Step-1 : Decrement Intervals By xyz Seconds In Cache
        MonitorCache.decrementMetricGroupInterval(CHECKING_INTERVAL);

        // Step-2 : After Decrementing Check For Timed-Out MetricGroups
        List<JsonObject> timedOutGroups = MonitorCache.getTimedOutMetricGroups();

        // Step-3 : If There are Timed-out MetricGroups Ready For Polling...
        if (!timedOutGroups.isEmpty())
        {
            // Step-4 : Format Request For Polling
            JsonArray metricGroups = preparePollingMetricGroups(timedOutGroups);

            // Step-5: Send Request to PluginManager
            PluginManager
                    .runPolling(metricGroups)
                    // ===== If Success : Save Results to DB =====
                    .onSuccess(this::processAndSaveResults)
                    .onFailure(err -> ConsoleLogger.error("❌ Error During Polling => " + err.getMessage()));
        }
    }

    // ===== Prepares Polling Request For Plugin =====
    private JsonArray preparePollingMetricGroups(List<JsonObject> timedOutGroups)
    {
        JsonArray metricGroups = new JsonArray();

        for (JsonObject metricGroup : timedOutGroups)
        {
            JsonObject groupData = new JsonObject()
                    .put(Fields.PluginPollingRequest.MONITOR_ID, metricGroup.getInteger(Fields.MonitorCache.MONITOR_ID))
                    .put(Fields.PluginPollingRequest.NAME, metricGroup.getString(Fields.MonitorCache.NAME))
                    .put(Fields.PluginPollingRequest.IP, metricGroup.getString(Fields.MonitorCache.IP))
                    .put(Fields.PluginPollingRequest.PORT, metricGroup.getInteger(Fields.MonitorCache.PORT))
                    .put(Fields.PluginPollingRequest.CREDENTIALS, metricGroup.getJsonObject(Fields.MonitorCache.CREDENTIALS));

            metricGroups.add(groupData);

        }

        return metricGroups;
    }

    // ===== Process Plugin Manager's Response & Save The Results In DB =====
    private void processAndSaveResults(JsonArray results)
    {
        List<Tuple> batchParams = new ArrayList<>();

        for (int i = 0; i < results.size(); i++)
        {
            JsonObject result = results.getJsonObject(i);

            // Step-1 : Skip unsuccessful results
            if (!result.getBoolean("success", false))
            {
                continue;
            }

            // Step-2 : Add Timestamp
            result.put(Fields.PollingResult.TIME, ZonedDateTime.now().toString());

            // Step-3 : Format Data Into JsonArray or JsonObject
            if(!getJsonObject(result.getString(Fields.PluginPollingResponse.DATA)).isEmpty())
            {
                batchParams.add(Tuple.of(
                        result.getInteger(Fields.PluginPollingResponse.MONITOR_ID),
                        result.getString(Fields.PluginPollingResponse.NAME),
                        getJsonObject(result.getString(Fields.PluginPollingResponse.DATA))
                ));
            }
            else if(!getJsonArray(result.getString(Fields.PluginPollingResponse.DATA)).isEmpty())
            {
                batchParams.add(Tuple.of(
                        result.getInteger(Fields.PluginPollingResponse.MONITOR_ID),
                        result.getString(Fields.PluginPollingResponse.NAME),
                        getJsonArray(result.getString(Fields.PluginPollingResponse.DATA))
                ));
            }
            else {
                batchParams.add(Tuple.of(
                        result.getInteger(Fields.PluginPollingResponse.MONITOR_ID),
                        result.getString(Fields.PluginPollingResponse.NAME),
                        result.getString(Fields.PluginPollingResponse.DATA)
                ));
            }
        }

        // Step-4 : Save Into DB
        if (!batchParams.isEmpty())
        {
            DbEngine.execute(Queries.PollingResult.INSERT, batchParams)
                    .onFailure(err -> ConsoleLogger.error("❌ Error During Saving Polled Data  => " + err.getMessage()));
        }
    }

    private JsonObject getJsonObject(String s)
    {
        try
        {
            return new JsonObject(s);
        }
        catch (Exception e)
        {
            return new JsonObject();
        }
    }

    private JsonArray getJsonArray(String s)
    {
        try
        {
            return new JsonArray(s);

        }
        catch (Exception e)
        {
            return new JsonArray();
        }
    }
}