package org.nms.validators;

import inet.ipaddr.IPAddressString;
import io.vertx.ext.web.RoutingContext;
import org.nms.constants.Config;

import static org.nms.App.LOGGER;
import static org.nms.utils.ApiUtils.sendFailure;

public class Validators
{
    private static final String ID = "id";

    public static final String PORT = "port";

    public static int validateID(RoutingContext ctx)
    {

        var id = ctx.request().getParam(ID);

        if (id == null || id.isEmpty())
        {
            sendFailure(ctx, 400,"Missing or empty 'id' parameter");

            return -1;
        }

        try
        {
            return Integer.parseInt(id);
        }
        catch (IllegalArgumentException exception)
        {
            sendFailure(ctx, 400,"Invalid 'id' parameter. Must be a positive integer");

            return -1;
        }
    }

    public static boolean validateBody(RoutingContext ctx)
    {
        try
        {
            var body = ctx.body().asJsonObject();

            if (body == null || body.isEmpty())
            {
                sendFailure(ctx, 400, "Missing or empty request body");

                return true;
            }
        }
        catch (Exception exception)
        {
            sendFailure(ctx, 400, "Invalid Json Body");

            return true;
        }

        return false;
    }

    public static boolean validateInputFields(RoutingContext ctx, String[] requiredFields, boolean isAllRequired)
    {

        try {

            var body = ctx.body().asJsonObject();

            var missingFields = new StringBuilder();

            var isAllMissing = true;

            for (var requiredField : requiredFields)
            {
                if (body.getString(requiredField) == null)
                {
                    missingFields.append(requiredField).append(", ");
                }
                else
                {
                    isAllMissing = false;
                }
            }

            if (isAllRequired && !missingFields.isEmpty())
            {
                sendFailure(ctx, 400, "Missing required fields: " + missingFields);

                return true;
            }

            if (!isAllRequired && isAllMissing)
            {
                sendFailure(ctx, 400, "Missing required fields, Provide any of " + missingFields);

                return true;
            }

        }
        catch (Exception exception)
        {
            sendFailure(ctx, 400, "Invalid Json");

            return true;
        }

        return false;
    }

    public static boolean validatePort(RoutingContext ctx)
    {
        var port = ctx.body().asJsonObject().getInteger(PORT);

        if (port != null && (port < 1 || port > 65535))
        {
            sendFailure(ctx, 400, "Port must be between 1 and 65535");

            return false;
        }

        return true;
    }

    public static boolean validateIpWithIpType(String ip, String ipType)
    {
        if (ip == null || ipType == null)
        {
            return true;
        }

        try
        {
            ipType = ipType.toUpperCase();

            switch (ipType)
            {
                case "SINGLE":

                    var singleAddr = new IPAddressString(ip).getAddress();

                    return ! (singleAddr != null && singleAddr.isIPv4() && singleAddr.getCount().longValue() == 1 );

                case "RANGE":

                    if (!ip.contains("-"))
                    {
                        return true;
                    }

                    var ipParts = ip.split("-");

                    if (ipParts.length != 2)
                    {
                        return true;
                    }

                    var startIp = new IPAddressString(ipParts[0].trim()).getAddress();

                    var endIp = new IPAddressString(ipParts[1].trim()).getAddress();

                    if (startIp == null || endIp == null || !startIp.isIPv4() || !endIp.isIPv4())
                    {
                        return true;
                    }

                    if (startIp.compareTo(endIp) > 0)
                    {
                        return true;
                    }

                    var range = startIp.toSequentialRange(endIp);

                    return ! ( range.getCount().longValue() <= Config.MAX_IP_COUNT );

                case "CIDR":

                    if (!ip.contains("/"))
                    {
                        return true;
                    }

                    var cidr = new IPAddressString(ip).getAddress();

                    if (cidr == null || !cidr.isIPv4() || !cidr.isPrefixed())
                    {
                        return true;
                    }

                    return ! ( cidr.getCount().longValue() <= Config.MAX_IP_COUNT );

                default:

                    return true;
            }
        }
        catch (Exception exception)
        {
            LOGGER.error("Failed to validate ip and ipType, error: " + exception.getMessage() );

            return true;
        }
    }

}
