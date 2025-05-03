package org.nms.HttpServer.Middlewares.Validators;

import io.vertx.ext.web.RoutingContext;
import org.nms.ConsoleLogger;
import org.nms.HttpServer.Utility.HttpResponse;

/**
 * Validates requests to user-related endpoints
 */
public class UserRequestValidator
{

    public static void getUserByIdRequestValidator(RoutingContext ctx)
    {

        ConsoleLogger.debug(ctx.request().method() + " " + ctx.request().path() + " "  + ctx.body().asJsonObject());

        // Check if id is present in the request

        Utility.isValidId(ctx, ctx.request().getParam("id"));

        ctx.next();

    }

    public static void registerRequestValidator(RoutingContext ctx)
    {

        ConsoleLogger.debug(ctx.request().method() + " " + ctx.request().path() + " "  + ctx.body().asJsonObject());

        // Check if body is present in the request

        var body = ctx.body().asJsonObject();

        if (body == null || body.isEmpty())
        {
            HttpResponse.sendFailure(ctx, 400,"Missing or empty request body");
            return;
        }

        // Check if all required fields are present in the body

        Utility.validateInputFields(ctx, body, new String[]{"username", "password", "confirmPassword"}, true);

        // Check if username, password and confirmPassword are in correct format

        if(body.getString("password").length() < 8)
        {
            HttpResponse.sendFailure(ctx, 400,"Password must be at least 8 characters long");
        }

        if(!body.getString("password").equals(body.getString("confirmPassword")))
        {
            HttpResponse.sendFailure(ctx, 400,"Password and confirm password do not match");
        }

        if(body.getString("email") != null && !body.getString("email").matches(Utility.EMAIL_REGEX))
        {
            HttpResponse.sendFailure(ctx, 400,"Email is not valid");
            return;
        }

        if(body.getString("username").length() < 3 || body.getString("username").length() > 50)
        {
            HttpResponse.sendFailure(ctx, 400,"Username must be between 3 and 50 characters");
            return;
        }

        ctx.next();
    }

    public static void loginRequestValidator(RoutingContext ctx)
    {
        ConsoleLogger.debug(ctx.request().method() + " " + ctx.request().path() + " "  + ctx.body().asJsonObject());

        // Check if body is present in the request

        var body = ctx.body().asJsonObject();

        if (body == null || body.isEmpty())
        {
            HttpResponse.sendFailure(ctx, 400,"Missing or empty request body");
            return;
        }

        // Check if all required fields are present in the body

        Utility.validateInputFields(ctx, body, new String[]{"username", "password"}, true);

        ctx.next();

    }

    public static void updateUserRequestValidator(RoutingContext ctx)
    {

        ConsoleLogger.debug(ctx.request().method() + " " + ctx.request().path() + " "  + ctx.body().asJsonObject());

        // Check if id is present in the request

        var id = ctx.request().getParam("id");

        Utility.isValidId(ctx, id);

        // Check if body is present in the request

        var body = ctx.body().asJsonObject();

        if(body == null || body.isEmpty())
        {
            HttpResponse.sendFailure(ctx, 400,"Request body is required");
            return;
        }

        // Check if atleast one field is present in the body

        Utility.validateInputFields(ctx, body, new String[]{"username", "password", "email"}, false);

        // Check if username, password and confirmPassword are in correct format

        var username = body.getString("username");

        var password = body.getString("password");

        var email = body.getString("email");

        if(username != null && (username.length() < 3 || username.length() > 50))
        {
            HttpResponse.sendFailure(ctx, 400,"Username must be 3 - 50 characters long");
        }

        if(password != null && (password != null && password.length() < 8))
        {
            HttpResponse.sendFailure(ctx, 400,"Password must be at least 8 characters long");
            return;
        }

        if(email != null && (!email.matches(Utility.EMAIL_REGEX)))
        {
            HttpResponse.sendFailure(ctx, 400,"Email is not valid");
            return;
        }

        ctx.next();
    }


    public static void deleteUserRequestValidator(RoutingContext ctx)
    {
        ConsoleLogger.debug(ctx.request().method() + " " + ctx.request().path() + " "  + ctx.body().asJsonObject());

        // Check if id is present in the request

        var id = ctx.request().getParam("id");

        Utility.isValidId(ctx, id);

        ctx.next();
    }
}
