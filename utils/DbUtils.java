package org.nms.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nms.constants.DatabaseConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbUtils
{
    private static final Logger log = LoggerFactory.getLogger(DbUtils.class);

    public static JsonObject buildRequest(String query, JsonArray data, String mode)
    {
        var request = new JsonObject()
                .put(DatabaseConstant.QUERY, query)
                .put(DatabaseConstant.MODE, mode);

        if(mode.equals(DatabaseConstant.MODE_SINGLE))
        {
            request.put(DatabaseConstant.DATA, data);
        }
        else if(mode.equals(DatabaseConstant.MODE_BATCH))
        {
            request.put(DatabaseConstant.BATCH_DATA, data);
        }
    }
}