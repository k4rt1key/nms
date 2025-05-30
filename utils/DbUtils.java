package org.nms.utils;

import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;
import static org.nms.App.LOGGER;
import static org.nms.App.VERTX;
import static org.nms.constants.Database.Common.*;

import org.nms.constants.Fields;

import java.util.List;
import java.util.stream.Collectors;

public class DbUtils
{
    public static JsonObject buildRequest(String tableName, String operation, JsonArray conditions, JsonObject data)
    {
        return new JsonObject()
                .put(TABLE_NAME, tableName)
                .put(OPERATION, operation)
                .put(CONDITION, conditions)
                .put(DATA, data);
    }
}