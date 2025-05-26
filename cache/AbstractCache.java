package org.nms.cache;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;

public interface AbstractCache
{
    Future<JsonArray> init();

    void insert(JsonArray items);

    void update(JsonArray items);

    void delete(Integer id);
}
