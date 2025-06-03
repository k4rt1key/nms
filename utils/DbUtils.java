package org.nms.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nms.constants.DatabaseConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbUtils
{
    public static JsonObject buildRequest(String query, JsonArray data, String mode)
    {
        var request = new JsonObject()
                .put(DatabaseConstant.QUERY, query)
                .put(DatabaseConstant.MODE, mode);

        if(mode.equals(DatabaseConstant.SQL_QUERY_SINGLE_PARAM))
        {
            request.put(DatabaseConstant.DATA, data);
        }
        else if(mode.equals(DatabaseConstant.SQL_QUERY_BATCH))
        {
            request.put(DatabaseConstant.BATCH_DATA, data);
        }
    }
}