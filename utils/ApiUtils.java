package org.nms.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.nms.constants.Configuration;

public class ApiUtils
{
    public static void sendSuccess(RoutingContext ctx, int statusCode, JsonArray data)
    {
        ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(data.encode());

    }

    public static void sendSuccess(RoutingContext ctx, int statusCode, JsonObject data)
    {
        ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(data.encode());

    }

    public static void sendFailure(RoutingContext ctx, int statusCode, String message)
    {
        var response = ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json");

        var json = new JsonObject()
                .put("message", message);

        response.end(json.toBuffer());
    }

    public static void sendFailure(RoutingContext ctx, int statusCode, String message, String error)
    {
        var response = ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json");

        var json = new JsonObject()
                .put("message", message)
                .put("error", Configuration.PRODUCTION ? message : error);

        response.end(json.toBuffer());
    }
}
