package org.nms.utils;

import inet.ipaddr.IPAddressString;
import io.vertx.core.json.JsonArray;
import org.nms.validators.Validators;

import static org.nms.App.LOGGER;

public class DiscoveryUtils
{
    public static JsonArray getIpsFromString(String ip, String ipType)
    {
        var ips = new JsonArray();

        if (Validators.validateIpWithIpType(ip, ipType)) return ips;

        try
        {
            if ("RANGE".equalsIgnoreCase(ipType))
            {
                var parts = ip.split("-");

                var startIp = new IPAddressString(parts[0].trim()).getAddress();

                var endIp = new IPAddressString(parts[1].trim()).getAddress();

                var address = startIp.toSequentialRange(endIp);

                address.getIterable().forEach(ipAddress -> ips.add(ipAddress.toString()));
            }
            else if ("CIDR".equalsIgnoreCase(ipType))
            {
                new IPAddressString(ip)
                        .getSequentialRange()
                        .getIterable()
                        .forEach(ipAddress -> ips.add(ipAddress.toString()));
            }
            else if ("SINGLE".equalsIgnoreCase(ipType))
            {
                var singleAddr = new IPAddressString(ip).getAddress();

                ips.add(singleAddr.toString());
            }

            return ips;
        }
        catch (Exception exception)
        {
            LOGGER.error("Failed to convert ip string to JsonArray of ip, error: " + exception.getMessage() );

            return new JsonArray();
        }
    }
}
