package org.nms.cache;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;

public interface AbstractCache
{
    Future<JsonArray> init();

    void insert(JsonArray monitorArray);

    void update(JsonArray metricGroups);

    void delete(Integer monitorId);

}
