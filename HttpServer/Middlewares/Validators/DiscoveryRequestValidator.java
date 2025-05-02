package org.nms.HttpServer.Middlewares.Validators;

import io.vertx.ext.web.RoutingContext;
import org.nms.ConsoleLogger;
import org.nms.HttpServer.Utility.HttpResponse;
import org.nms.HttpServer.Utility.IpUtility;

/**
 * Validates requests to discovery-related endpoints
 */
public class DiscoveryRequestValidator
{
    public static void getDiscoveryByIdRequestValidator(RoutingContext ctx)
    {
        ConsoleLogger.debug(ctx.request().method() + " " + ctx.request().path() + " "  + ctx.body().asJsonObject());

        // Check if id is present in the request
        Utility.isValidId(ctx, ctx.request().getParam("id"));

        ctx.next();
    }

    public static void createDiscoveryRequestValidator(RoutingContext ctx)
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
        Utility.validateInputFields(ctx, body, new String[]{"name", "ip", "ip_type", "credentials", "port"}, true);

        // Validate name format
        var name = body.getString("name");
        if (name.length() < 3 || name.length() > 50)
        {
            HttpResponse.sendFailure(ctx, 400, "Name must be between 3 and 50 characters");
            return;
        }

        // Validate credentials
        var credentials = body.getJsonArray("credentials");
        if (credentials == null || credentials.isEmpty())
        {
            HttpResponse.sendFailure(ctx, 400, "Missing or empty 'credentials' field");
            return;
        }

        for (var credential : credentials)
        {
            try {
                Integer.parseInt(credential.toString());
            } catch (NumberFormatException e) {
                HttpResponse.sendFailure(ctx, 400, "Invalid credential ID: " + credential);
                return;
            }
        }

        // Validate port
        var port = body.getInteger("port");
        if (port < 1 || port > 65535)
        {
            HttpResponse.sendFailure(ctx, 400, "Port must be between 1 and 65535");
            return;
        }

        // Validate IP type
        var ipType = body.getString("ip_type");
        if (!ipType.equals("SINGLE") && !ipType.equals("RANGE") && !ipType.equals("SUBNET"))
        {
            HttpResponse.sendFailure(ctx, 400, "Invalid IP type. Only 'SINGLE', 'RANGE' and 'SUBNET' are supported");
            return;
        }

        // Validate IP address
        var ip = body.getString("ip");
        if (!IpUtility.isValidIpAndType(ip, ipType))
        {
            HttpResponse.sendFailure(ctx, 400, "Invalid IP address or mismatch Ip and IpType: " + ip + ", " + ipType);
            return;
        }

        ctx.next();
    }

    public static void updateDiscoveryRequestValidator(RoutingContext ctx)
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
        Utility.validateInputFields(ctx, body, new String[]{"name", "ip", "ip_type", "port"}, false);

        // Validate name format if present
        var name = body.getString("name");
        if (name != null && (name.length() < 3 || name.length() > 50))
        {
            HttpResponse.sendFailure(ctx, 400, "Name must be between 3 and 50 characters");
            return;
        }

        // Validate port if present
        var port = body.getInteger("port");
        if (port != null && (port < 1 || port > 65535))
        {
            HttpResponse.sendFailure(ctx, 400, "Port must be between 1 and 65535");
            return;
        }

        // Validate IP type if present
        var ipType = body.getString("ip_type");
        if (ipType != null && !ipType.equals("SINGLE") && !ipType.equals("RANGE") && !ipType.equals("SUBNET"))
        {
            HttpResponse.sendFailure(ctx, 400, "Invalid IP type. Only 'SINGLE', 'RANGE' and 'SUBNET' are supported");
            return;
        }

        // Validate IP address if both ip and ipType are present
        var ip = body.getString("ip");
        if (ip != null && ipType != null && !IpUtility.isValidIpAndType(ip, ipType))
        {
            HttpResponse.sendFailure(ctx, 400, "Invalid IP address or mismatch Ip and IpType: " + ip + ", " + ipType);
            return;
        }

        ctx.next();
    }

    public static void updateDiscoveryCredentialsRequestValidator(RoutingContext ctx)
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

        var add_credentials = body.getJsonArray("add_credentials");
        var remove_credentials = body.getJsonArray("remove_credentials");

        if ((add_credentials == null || add_credentials.isEmpty()) &&
                (remove_credentials == null || remove_credentials.isEmpty()))
        {
            HttpResponse.sendFailure(ctx, 400, "Missing or empty 'add_credentials' and 'remove_credentials' fields");
            return;
        }

        // Validate add_credentials if present
        if (add_credentials != null && !add_credentials.isEmpty())
        {
            for (var credential : add_credentials)
            {
                try {
                    Integer.parseInt(credential.toString());
                } catch (NumberFormatException e) {
                    HttpResponse.sendFailure(ctx, 400, "Invalid credential ID in add_credentials: " + credential);
                    return;
                }
            }
        }

        // Validate remove_credentials if present
        if (remove_credentials != null && !remove_credentials.isEmpty())
        {
            for (var credential : remove_credentials)
            {
                try {
                    Integer.parseInt(credential.toString());
                } catch (NumberFormatException e) {
                    HttpResponse.sendFailure(ctx, 400, "Invalid credential ID in remove_credentials: " + credential);
                    return;
                }
            }
        }

        ctx.next();
    }

    public static void runDiscoveryRequestValidator(RoutingContext ctx)
    {
        ConsoleLogger.debug(ctx.request().method() + " " + ctx.request().path() + " "  + ctx.body().asJsonObject());

        // Check if id is present in the request
        Utility.isValidId(ctx, ctx.request().getParam("id"));

        ctx.next();
    }

    public static void deleteDiscoveryRequestValidator(RoutingContext ctx)
    {
        ConsoleLogger.debug(ctx.request().method() + " " + ctx.request().path() + " "  + ctx.body().asJsonObject());

        // Check if id is present in the request
        Utility.isValidId(ctx, ctx.request().getParam("id"));

        ctx.next();
    }
}