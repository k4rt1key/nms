package org.nms.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.logging.Logger;

import static org.nms.App.LOGGER;
import static org.nms.constants.Database.Common.*;

public class DbUtils
{
    public static JsonObject buildRequest(String tableName, String operation, JsonArray conditions, JsonObject data, JsonArray batchData)
    {
        return new JsonObject()
                .put(TABLE_NAME, tableName)
                .put(OPERATION, operation)
                .put(CONDITION, conditions)
                .put(BATCH_DATA, batchData)
                .put(DATA, data);
    }
}