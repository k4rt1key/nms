package org.nms.Database.Services;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;

public interface BaseDatabaseService
{
    Future<JsonArray> get(JsonArray params);

    Future<JsonArray> getAll();

    Future<JsonArray> save(JsonArray params);

    Future<JsonArray> update(JsonArray params);

    Future<JsonArray> delete(JsonArray params);

    Future<Void> createSchema();
}
