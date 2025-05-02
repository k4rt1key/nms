package org.nms.HttpServer.Middlewares.Validators;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.nms.HttpServer.Utility.HttpResponse;

public class Utility
{
    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";

    public static boolean isValidId(RoutingContext ctx, String id)
    {
        if (id == null || id.isEmpty())
        {
            HttpResponse.sendFailure(ctx, 400,"Missing or empty 'id' parameter");
            return false;
        }

        // Check if the ID is a valid Integer > 0
        try {
            Integer.parseInt(id);
            return true;
        }
        catch (IllegalArgumentException e)
        {
            HttpResponse.sendFailure(ctx, 400,"Invalid 'id' parameter. Must be a positive integer");
            return false;
        }
    }

    public static boolean validateInputFields(RoutingContext ctx, JsonObject body, String[] requiredFields, boolean isAllRequired)
    {

        var missingFields = new StringBuilder();

        var isAllMissing = true;

        for (String requiredField : requiredFields) {
            if (body.getString(requiredField) == null) {
                isAllMissing = false;
                missingFields
                        .append(requiredField)
                        .append(", ");
            }
        }

        if(isAllRequired && missingFields.length() > 0)
        {
            HttpResponse.sendFailure(ctx, 400,"Missing required fields: " + missingFields);
            return false;
        }

        if(!isAllRequired && isAllMissing)
        {
            HttpResponse.sendFailure(ctx, 400,"Missing required fields: " + missingFields);
            return false;
        }

        return true;
    }
}
