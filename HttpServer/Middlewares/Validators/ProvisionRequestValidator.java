package org.nms.HttpServer.Middlewares.Validators;

import io.vertx.ext.web.RoutingContext;
import org.nms.HttpServer.Utility.HttpResponse;
import org.nms.HttpServer.Utility.IpUtility;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

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

        var requiredFields = new String[]{"metrics"};

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

        var metrics = body.getJsonArray("metrics");

        if(metrics != null && !metrics.isEmpty())
        {
            for(var i = 0; i < metrics.size(); i++)
            {
                var type = metrics.getJsonObject(i).getString("name");

                var MetricType = new ArrayList<String>(List.of("CPU", "MEMORY", "DISK", "PROCCESS", "SYSINFO", "PING", "NETWORK"));
                if(!MetricType.contains(type))
                {
                    HttpResponse.sendFailure(ctx, 400,"Invalid Metric Type " + type);
                    return;
                }

                var interval = metrics.getJsonObject(i).getInteger("polling_interval");

                var enable = metrics.getJsonObject(i).getBoolean("enable");

                if(type == null)
                {
                    HttpResponse.sendFailure(ctx, 400,"Provide Valid Metric Type");
                    return;
                }

                if( ( (interval == null || interval <= 10 ) & enable == null ) )
                {
                    HttpResponse.sendFailure(ctx, 400,"Provide interval ( Multiply of 10 ) or enable flag for given type");
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
