package org.nms.API.Middlewares.Validators;

import io.vertx.ext.web.RoutingContext;
import org.nms.ConsoleLogger;
import org.nms.API.Utility.HttpResponse;

/**
 * Validates requests to credential-related endpoints
 */
public class CredentialRequestValidator
{
    public static void getCredentialByIdRequestValidator(RoutingContext ctx)
    {
        ConsoleLogger.debug(ctx.request().method() + " " + ctx.request().path() + " "  + ctx.body().asJsonObject());

        Utility.isValidId(ctx, ctx.request().getParam("id"));

        ctx.next();
    }

    public static void createCredentialRequestValidator(RoutingContext ctx)
    {
        ConsoleLogger.debug(ctx.request().method() + " " + ctx.request().path() + " "  + ctx.body().asJsonObject());

        // Check if body is present in the request
        var body = ctx.body().asJsonObject();

        if (body == null || body.isEmpty())
        {
            HttpResponse.sendFailure(ctx, 400, "Missing or empty request body");
            return;
        }

        // Check if all required fields are present in the body
        Utility.validateInputFields(ctx, body, new String[]{"name", "type", "username", "password"}, true);

        // Validate name format
        if(body.getString("name") != null && (body.getString("name").length() < 3 || body.getString("name").length() > 50))
        {
            HttpResponse.sendFailure(ctx, 400, "Name must be between 3 and 50 characters");
            return;
        }

        // Validate credential type
        if(body.getString("type") != null && !body.getString("type").equals("WINRM"))
        {
            HttpResponse.sendFailure(ctx, 400, "Invalid type. Only 'WINRM' is supported for now");
            return;
        }

        ctx.next();
    }

    public static void updateCredentialByIdRequestValidator(RoutingContext ctx)
    {
        ConsoleLogger.debug(ctx.request().method() + " " + ctx.request().path() + " "  + ctx.body().asJsonObject());

        // Check if id is present in the request
        Utility.isValidId(ctx, ctx.request().getParam("id"));

        // Check if body is present in the request
        var body = ctx.body().asJsonObject();

        if (body == null || body.isEmpty())
        {
            HttpResponse.sendFailure(ctx, 400, "Missing or empty request body");
            return;
        }

        // Check if at least one field is present in the body
        Utility.validateInputFields(ctx, body, new String[]{"name", "type", "username", "password"}, false);

        // Validate name format if present
        if(body.getString("name") != null && (body.getString("name").length() < 3 || body.getString("name").length() > 50))
        {
            HttpResponse.sendFailure(ctx, 400, "Name must be between 3 and 50 characters");
            return;
        }

        // Validate credential type if present
        if(body.getString("type") != null && !body.getString("type").equals("WINRM"))
        {
            HttpResponse.sendFailure(ctx, 400, "Invalid type. Only 'WINRM' is supported for now");
            return;
        }

        ctx.next();
    }

    public static void deleteCredentialByIdRequestValidator(RoutingContext ctx)
    {
        ConsoleLogger.debug(ctx.request().method() + " " + ctx.request().path() + " "  + ctx.body().asJsonObject());

        // Check if id is present in the request
        Utility.isValidId(ctx, ctx.request().getParam("id"));

        ctx.next();
    }
}