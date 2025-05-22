package org.nms.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.nms.constants.Config;

public class DbUtils
{
    public static void sendSuccess(RoutingContext ctx, int statusCode, String message, JsonArray data)
    {
        var response = ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json");

        var json = new JsonObject()
                .put("success", true)
                .put("statusCode", statusCode)
                .put("message", message)
                .put("data", data);

        response.end(json.toBuffer());
    }

    public static void sendFailure(RoutingContext ctx, int statusCode, String message)
    {
        var response = ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json");

        var json = new JsonObject()
                .put("success", false)
                .put("statusCode", statusCode)
                .put("message", message);

        response.end(json.toBuffer());
    }

    public static void sendFailure(RoutingContext ctx, int statusCode, String message, String error)
    {
        var response = ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json");

        var json = new JsonObject()
                .put("success", false)
                .put("statusCode", statusCode)
                .put("message", message)
                .put("error", Config.PRODUCTION ? message : error);

        response.end(json.toBuffer());
    }
}
