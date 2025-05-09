package org.nms.api.helpers;

import io.vertx.core.json.JsonArray;

import java.net.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Ip
{
    private static final int MAX_IP_COUNT = 1024;

    // Returns a JSON array of IPs based on the input IP and type
    public static JsonArray getIpListAsJsonArray(String ip, String ipType)
    {
        JsonArray jsonArray = new JsonArray();
        List<String> ipList = null;

        if (ip == null || ipType == null)
        {
            return jsonArray;
        }

        switch (ipType.toUpperCase())
        {
            case "SINGLE":
                if (isValidIp(ip))
                {
                    jsonArray.add(ip);
                }
                break;

            case "RANGE":
                if (ip.contains("-"))
                {
                    String[] range = ip.split("-");
                    if (range.length == 2)
                    {
                        String startIp = range[0].trim();
                        String endIp = range[1].trim();
                        ipList = getIpListForRange(startIp, endIp);
                    }
                }
                break;

            case "CIDR":
                if (ip.contains("/"))
                {
                    String[] CIDR = ip.split("/");

                    if (CIDR.length == 2)
                    {
                        String ipPart = CIDR[0].trim();
                        try
                        {
                            int prefixLength = Integer.parseInt(CIDR[1].trim());
                            ipList = getIpListForCIDR(ipPart, prefixLength);
                        }
                        catch (NumberFormatException e)
                        {
                            // Invalid prefix length
                        }
                    }
                }
                break;

            default:
                break;
        }

        if (ipList != null && !ipList.isEmpty())
        {
            for (String ipAddress : ipList)
            {
                jsonArray.add(ipAddress);
            }
        }

        return jsonArray;
    }

    // Check if IP address is valid
    public static boolean isValidIp(String ip)
    {
        if (ip == null || ip.isEmpty())
        {
            return false;
        }

        try
        {
            InetAddress.getByName(ip);

            return true;
        }
        catch (UnknownHostException e)
        {
            return false;
        }
    }

    // Check if the IP address and its type are valid
    public static boolean isValidIpAndType(String ip, String ipType)
    {
        if (ip == null || ipType == null)
        {
            return true;
        }

        switch (ipType)
        {
            case "SINGLE":
                return !isValidIp(ip);

            case "RANGE":
                if (!ip.contains("-"))
                {
                    return true;
                }

                String[] range = ip.split("-");

                if (range.length != 2)
                {
                    return true;
                }

                String startIp = range[0].trim();
                String endIp = range[1].trim();

                if (!isValidIp(startIp) || !isValidIp(endIp))
                {
                    return true;
                }

                try
                {
                    InetAddress startAddr = InetAddress.getByName(startIp);
                    InetAddress endAddr = InetAddress.getByName(endIp);

                    if (startAddr.getAddress().length != endAddr.getAddress().length)
                    {
                        return true;
                    }

                    var ipList = getIpListForRange(startIp, endIp);
                    return ipList == null || ipList.size() > MAX_IP_COUNT;
                }
                catch (UnknownHostException e)
                {
                    return true;
                }

            case "CIDR":
                if (!ip.contains("/"))
                {
                    return true;
                }

                String[] CIDR = ip.split("/");

                if (CIDR.length != 2)
                {
                    return true;
                }

                String ipPart = CIDR[0].trim();

                if (!isValidIPv4(ipPart))
                {
                    return true;
                }

                try
                {
                    int prefixLength = Integer.parseInt(CIDR[1].trim());

                    if (prefixLength < 0 || prefixLength > 32)
                    {
                        return true;
                    }

                    long ipCount = 1L << (32 - prefixLength);

                    if (ipCount > MAX_IP_COUNT)
                    {
                        return true;
                    }

                    var ipList = getIpListForCIDR(ipPart, prefixLength);
                    return ipList == null || ipList.size() > MAX_IP_COUNT;
                }
                catch (NumberFormatException e)
                {
                    return true;
                }

            default:
                return true;
        }
    }

    // Check if IPv4 address is valid
    public static boolean isValidIPv4(String ip)
    {
        if (ip == null || ip.isEmpty())
        {
            return false;
        }

        try
        {
            InetAddress inetAddress = InetAddress.getByName(ip);
            return inetAddress.getAddress().length == 4;
        }
        catch (UnknownHostException e)
        {
            return false;
        }
    }

    // Get list of IPs for a given range
    public static List<String> getIpListForRange(String startIp, String endIp)
    {
        try
        {
            InetAddress startAddr = InetAddress.getByName(startIp);
            InetAddress endAddr = InetAddress.getByName(endIp);

            byte[] startBytes = startAddr.getAddress();
            byte[] endBytes = endAddr.getAddress();

            BigInteger startNum = new BigInteger(1, startBytes);
            BigInteger endNum = new BigInteger(1, endBytes);

            if (startNum.compareTo(endNum) > 0)
            {
                return null;
            }

            BigInteger rangeSize = endNum.subtract(startNum).add(BigInteger.ONE);
            if (rangeSize.compareTo(BigInteger.valueOf(MAX_IP_COUNT)) > 0)
            {
                return null;
            }

            List<String> ipList = new ArrayList<>();

            BigInteger currentNum = startNum;
            while (currentNum.compareTo(endNum) <= 0)
            {
                byte[] ipBytes = bigIntegerToBytes(currentNum, startBytes.length);

                ipList.add(InetAddress.getByAddress(ipBytes).getHostAddress());

                currentNum = currentNum.add(BigInteger.ONE);
            }

            return ipList;
        }
        catch (UnknownHostException e)
        {
            return null;
        }
    }

    // Get list of IPs for a given CIDR
    public static List<String> getIpListForCIDR(String ip, int prefixLength)
    {
        try
        {
            InetAddress addr = InetAddress.getByName(ip);

            byte[] ipBytes = addr.getAddress();

            if (ipBytes.length != 4)
            {
                return null; // Only IPv4 supported
            }

            long ipCount = 1L << (32 - prefixLength);

            if (ipCount > MAX_IP_COUNT)
            {
                return null;
            }

            long ipNum = bytesToLong(ipBytes);
            long mask = (0xFFFFFFFFL << (32 - prefixLength)) & 0xFFFFFFFFL;
            long network = ipNum & mask;

            List<String> ipList = new ArrayList<>();

            for (long i = 0; i < ipCount; i++)
            {
                long currentIp = network + i;

                byte[] currentBytes = longToBytes(currentIp);

                ipList.add(InetAddress.getByAddress(currentBytes).getHostAddress());
            }
            return ipList;
        }
        catch (UnknownHostException e)
        {
            return null;
        }
    }

    // Convert byte array to long (for IPv4)
    private static long bytesToLong(byte[] bytes)
    {
        long result = 0;

        for (byte b : bytes)
        {
            result = (result << 8) | (b & 0xFF);
        }

        return result;
    }

    // Convert long to byte array (for IPv4)
    private static byte[] longToBytes(long value)
    {
        byte[] bytes = new byte[4];

        for (int i = 3; i >= 0; i--)
        {
            bytes[i] = (byte) (value & 0xFF);
            value >>= 8;
        }

        return bytes;
    }

    // Convert BigInteger to byte array with specified length
    private static byte[] bigIntegerToBytes(BigInteger num, int length)
    {
        byte[] bytes = num.toByteArray();

        byte[] result = new byte[length];

        int srcOffset = Math.max(0, bytes.length - length);

        int destOffset = length - Math.min(bytes.length, length);

        System.arraycopy(bytes, srcOffset, result, destOffset, Math.min(bytes.length, length));

        return result;
    }
}