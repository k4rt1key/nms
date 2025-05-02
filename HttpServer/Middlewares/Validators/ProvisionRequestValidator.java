package org.nms.HttpServer.Middlewares.Validators;

import io.vertx.ext.web.RoutingContext;
import org.nms.HttpServer.Utility.IpUtility;

public class ProvisionRequestValidator
{
    public static void getProvisionByIdRequestValidator(RoutingContext ctx)
    {
        var id = ctx.request().getParam("id");

        if(id == null || id.isEmpty())
        {
            ctx.response().setStatusCode(400).end("Missing or empty 'id' parameter");
            return;
        }

        ctx.next();
    }

    public static void createProvisionRequestValidator(RoutingContext ctx)
    {
        var body = ctx.body().asJsonObject();

        if(body == null || body.isEmpty())
        {
            ctx.response().setStatusCode(400).end("Missing or empty request body");
            return;
        }

        var requiredFields = new String[]{"ips", "discovery_id"};

        var missingFields = new StringBuilder();

        for (String field : requiredFields)
        {
            if (!body.containsKey(field))
            {
                missingFields.append(field).append(", ");
            }
        }

        if (missingFields.length() > 0
)
        {
            ctx.response().setStatusCode(400).end("Missing required fields: " + missingFields);
            return;
        }


        var ips = body.getJsonArray("ips");

        if(ips.size() > 1000)
        {
            ctx.response().setStatusCode(400).end("Too many IPs, maximum is 1000");
            return;
        }

        var invalidIps = new StringBuilder();

        for(var ip: ips)
        {
            if(!IpUtility.isValidIp(ip.toString()))
            {
                invalidIps.append(ip).append(", ");
            }
        }

        if(invalidIps.length() > 0)
        {
            ctx.response().setStatusCode(400).end("Invalid IPs: " + invalidIps);
            return;
        }

        if(body.getString("discovery_id").isEmpty())
        {
            ctx.response().setStatusCode(400).end("Discovery ID must be at least 1 character");
            return;
        }

        ctx.next();
    }

    public static void updateProvisionRequestValidator(RoutingContext ctx)
    {
        var id = ctx.request().getParam("id");

        if(id == null || id.isEmpty())
        {
            ctx.response().setStatusCode(400).end("Missing or empty 'id' parameter");
            return;
        }

        var body = ctx.body().asJsonObject();

        if(body == null || body.isEmpty())
        {
            ctx.response().setStatusCode(400).end("Missing or empty request body");
            return;
        }

        var requiredFields = new String[]{"add_metrics", "remove_metrics"};

        var isAllMissing = true;

        for(var field : requiredFields)
        {
            if(body.containsKey(field))
            {
                isAllMissing = false;
            }
        }

        if(isAllMissing)
        {
            ctx.response().setStatusCode(400).end("Missing required fields: " + String.join(" Or ", requiredFields));
            return;
        }

        var add_metrics = body.getJsonArray("add_metrics");

        if(add_metrics != null && !add_metrics.isEmpty())
        {
            for(var metric : add_metrics)
            {
                try
                {
                    Integer.parseInt(metric.toString());
                }
                catch (NumberFormatException e)
                {
                    ctx.response().setStatusCode(400).end("Invalid metric ID: " + metric);
                    return;
                }
            }
        }

        var remove_metrics = body.getJsonArray("remove_metrics");

        if(remove_metrics != null && !remove_metrics.isEmpty())
        {
            for(var metric : remove_metrics)
            {
                try
                {
                    Integer.parseInt(metric.toString());
                }
                catch (NumberFormatException e)
                {
                    ctx.response().setStatusCode(400).end("Invalid metric ID: " + metric);
                    return;
                }
            }
        }

        ctx.next();
    }

    public static void deleteProvisionRequestValidator(RoutingContext ctx)
    {
        var id = ctx.request().getParam("id");

        if(id == null || id.isEmpty())
        {
            ctx.response().setStatusCode(400).end("Missing or empty 'id' parameter");
            return;
        }

        ctx.next();
    }
}
