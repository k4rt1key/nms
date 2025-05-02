package org.nms.Cache;

import io.vertx.core.json.JsonObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MetricGroupCacheStore
{
    private static final ConcurrentHashMap<Integer, JsonObject> cachedMetricGroups = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Integer, JsonObject> referencedMetricGroups = new ConcurrentHashMap<>();

    public static void setCachedMetricGroup(Integer key, io.vertx.core.json.JsonObject value)
    {
        cachedMetricGroups.put(key, value);
    }

    public static void setReferencedMetricGroup(Integer key, JsonObject value)
    {
        referencedMetricGroups.put(key, value);
    }

    public static JsonObject getCachedMetricGroup(Integer id)
    {
        return cachedMetricGroups.get(id);
    }

    public static JsonObject getReferencedMetricGroup(Integer id)
    {
        return referencedMetricGroups.get(id);
    }


    public static List<JsonObject> getTimedOutMetricGroups() {
        List<JsonObject> timedOutMetricGroups = new ArrayList<>();

        cachedMetricGroups.forEach((key, value) -> {
            if (value.getInteger("interval") <= 0) {
                cachedMetricGroups.put(key, referencedMetricGroups.get(key));
                timedOutMetricGroups.add(value);
            }
        });

        return timedOutMetricGroups;
    }

    public static void decrementMetricGroupInterval(int interval)
    {
        cachedMetricGroups.replaceAll((k, v) -> v.put("interval", v.getInteger("interval") - interval));
    }
}
