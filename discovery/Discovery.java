package org.nms.discovery;

import inet.ipaddr.IPAddressString;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import static org.nms.App.LOGGER;

import io.vertx.core.net.NetClientOptions;
import io.vertx.sqlclient.Tuple;
import org.nms.App;
import org.nms.validators.Validators;
import org.nms.constants.Config;
import org.nms.constants.Fields;
import org.nms.constants.Queries;
import org.nms.utils.DbUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.nms.constants.Fields.Discovery.*;
import static org.nms.constants.Fields.Discovery.SUCCESS;
import static org.nms.constants.Fields.DiscoveryResult.MESSAGE;

public class Discovery extends AbstractVerticle
{

    @Override
    public void start()
    {
        vertx.eventBus().<JsonObject>localConsumer(Fields.EventBus.RUN_DISCOVERY_ADDRESS, message ->
        {
            var body =  message.body();

            var id = body.getInteger(Fields.Discovery.ID);

            runDiscoveryProcess(id)
                    .onComplete(asyncResult ->
                    {
                        if (asyncResult.failed())
                        {
                            LOGGER.error("Error running discovery id " + id + ": " + asyncResult.cause().getMessage());
                        }
                    });
        });

        LOGGER.debug("✅ Discovery Verticle Deployed, on thread [ " + Thread.currentThread().getName() + " ] ");
    }

    @Override
    public void stop()
    {
        LOGGER.info("\uD83D\uDED1 Discovery Verticle Stopped");
    }

    private Future<Void> runDiscoveryProcess(int id)
    {
        var promise = Promise.promise();

        // Step 1: Fetch discovery details
        fetchDiscoveryDetails(id)
                .compose(discovery ->
                {
                    // Step 2: Update discovery status to RUNNING
                    return updateDiscoveryStatus(id, Fields.Discovery.COMPLETED_STATUS)

                            .compose(statusUpdationResult ->
                            {
                                // Step 3: Delete existing results
                                return DbUtils.sendQueryExecutionRequest (
                                        Queries.Discovery.DELETE_RESULT,
                                        new JsonArray().add(id)
                                );

                            })

                            .compose(v ->
                            {
                                // Step 4: Perform discovery process
                                var ipStr = discovery.getString(Fields.Discovery.IP);

                                var ipType = discovery.getString(Fields.Discovery.IP_TYPE);

                                var port = discovery.getInteger(Fields.Discovery.PORT);

                                var credentials = discovery.getJsonArray(Fields.Discovery.CREDENTIAL_JSON);

                                var ips = getIpsFromString(ipStr, ipType);

                                return executeDiscoverySteps(id, ips, port, credentials);
                            });
                })

                .compose(discoveryRunResult ->
                {
                    // Step 5: Update discovery status to COMPLETED
                    return updateDiscoveryStatus(id, Fields.Discovery.COMPLETED_STATUS);
                })

                .onComplete(statusUpdationStatus ->
                {
                    if (statusUpdationStatus.succeeded())
                    {
                        promise.complete();
                    }
                    else
                    {
                        // If any step fails, update status to FAILED and complete with failure
                        updateDiscoveryStatus(id, Fields.DiscoveryResult.FAILED_STATUS)
                                .onComplete(updateStatusToFailResult -> promise.fail(statusUpdationStatus.cause().getMessage()));
                    }
                });

        return Future.succeededFuture();
    }

    private Future<Void> executeDiscoverySteps(int id, JsonArray ips, int port, JsonArray credentials)
    {
        Promise<Void> promise = Promise.promise();

        // Step 1: Ping Check - directly call without event bus
        pingIps(ips)
                .compose(pingResults ->
                {
                    var pingPassedIps = insertPingResults(id, pingResults);

                    // Step 2: Port Check - directly call without event bus
                    return checkPorts(pingPassedIps, port)

                            .compose(portResults ->
                            {
                                var portPassedIps = insertPortCheckResults(id, portResults);

                                // Step 3: Credentials Check - only for IPs that passed port check
                                if (portPassedIps.isEmpty())
                                {
                                    LOGGER.info("No IPs passed port check for discovery with id " + id);

                                    return Future.succeededFuture();
                                }

                                // Prepare plugin discovery request
                                var discoveryRequest = new JsonObject()
                                        .put(Fields.PluginDiscoveryRequest.TYPE, Fields.PluginDiscoveryRequest.DISCOVERY)

                                        .put(Fields.PluginDiscoveryRequest.ID, id)

                                        .put(Fields.PluginDiscoveryRequest.IPS, portPassedIps)

                                        .put(Fields.PluginDiscoveryRequest.PORT, port)

                                        .put(Fields.PluginDiscoveryRequest.CREDENTIALS, credentials);


                                // Send discovery request to plugin via event bus
                                return sendDiscoveryRequestToPlugin(discoveryRequest)
                                        .compose(asyncResult -> processCredentialCheckResults(id, asyncResult));
                            });
                })

                .onComplete(asyncResult ->
                {
                    if (asyncResult.succeeded())
                    {
                        promise.complete();
                    }
                    else
                    {
                        promise.fail(asyncResult.cause());
                    }
                });

        return promise.future();
    }

    private Future<JsonArray> sendDiscoveryRequestToPlugin(JsonObject request)
    {
        Promise<JsonArray> promise = Promise.promise();

        vertx.eventBus().<JsonArray>request(Fields.EventBus.PLUGIN_SPAWN_ADDRESS, request, reply ->
        {
            if (reply.succeeded())
            {
                var response = reply.result().body();

                promise.complete(response);
            }
            else
            {
                LOGGER.error("Error calling plugin: " + reply.cause().getMessage());

                promise.complete(new JsonArray());
            }
        });

        return promise.future();
    }

    private Future<JsonObject> fetchDiscoveryDetails(int id)
    {
        Promise<JsonObject> promise = Promise.promise();

        DbUtils.sendQueryExecutionRequest(Queries.Discovery.GET_BY_ID, new JsonArray().add(id))
                .onComplete(asyncResult ->
                {
                    if (asyncResult.succeeded())
                    {
                        var discoveryArray = asyncResult.result();

                        if (discoveryArray.isEmpty())
                        {
                            promise.fail("Discovery not found");
                        }
                        else
                        {
                            promise.complete(discoveryArray.getJsonObject(0));
                        }
                    }
                    else
                    {
                        promise.fail(asyncResult.cause());
                    }
                });

        return promise.future();
    }

    private Future<Void> updateDiscoveryStatus(int id, String status)
    {
        Promise<Void> promise = Promise.promise();

        DbUtils.sendQueryExecutionRequest(
                        Queries.Discovery.UPDATE_STATUS,
                        new JsonArray().add(id).add(status)
                )
                .onComplete(asyncResult ->
                {
                    if (asyncResult.succeeded())
                    {
                        promise.complete();
                    }
                    else
                    {
                        promise.fail(asyncResult.cause());
                    }
                });

        return promise.future();
    }

    private JsonArray insertPingResults(int id, JsonArray pingResults)
    {
        var pingCheckPassedIps = new JsonArray();

        var pingCheckFailedIps = new ArrayList<Tuple>();

        for (var i = 0; i < pingResults.size(); i++)
        {
            var result = pingResults.getJsonObject(i);

            var success = result.getBoolean(Fields.Discovery.SUCCESS);

            var ip = result.getString(Fields.Discovery.IP);

            if (success)
            {
                pingCheckPassedIps.add(ip);
            }
            else
            {
                pingCheckFailedIps.add(Tuple.of(id, null, ip, result.getString(Fields.DiscoveryResult.MESSAGE), Fields.Discovery.FAILED_STATUS));
            }
        }

        // Insert failed IPs if any
        if (!pingCheckFailedIps.isEmpty())
        {
            DbUtils.sendQueryExecutionRequest(Queries.Discovery.INSERT_RESULT, pingCheckFailedIps);
        }

        return pingCheckPassedIps;
    }

    private JsonArray insertPortCheckResults(int id, JsonArray portCheckResults)
    {
        var portCheckPassedIps = new JsonArray();

        var portCheckFailedIps = new ArrayList<Tuple>();

        for (int i = 0; i < portCheckResults.size(); i++)
        {
            var result = portCheckResults.getJsonObject(i);

            var success = result.getBoolean(Fields.Discovery.SUCCESS);

            var ip = result.getString(Fields.Discovery.IP);

            if (success)
            {
                portCheckPassedIps.add(ip);
            }
            else
            {
                portCheckFailedIps.add(Tuple.of(id, null, ip, result.getString(Fields.DiscoveryResult.MESSAGE), Fields.DiscoveryResult.FAILED_STATUS));
            }
        }

        // Insert failed IPs if any
        if (!portCheckFailedIps.isEmpty())
        {
            DbUtils.sendQueryExecutionRequest(Queries.Discovery.INSERT_RESULT, portCheckFailedIps);
        }

        return portCheckPassedIps;
    }

    private Future<Void> processCredentialCheckResults(int id, JsonArray credentialCheckResults)
    {
        var credentialCheckFailedIps = new ArrayList<Tuple>();

        var credentialCheckSuccessIps = new ArrayList<Tuple>();

        for (int i = 0; i < credentialCheckResults.size(); i++)
        {
            var result = credentialCheckResults.getJsonObject(i);

            var success = result.getBoolean(Fields.Discovery.SUCCESS);

            var ip = result.getString(Fields.PluginDiscoveryResponse.IP);

            if (success)
            {
                var credential = result.getJsonObject(Fields.PluginDiscoveryResponse.CREDENTIALS)
                        .getInteger(Fields.Credential.ID);

                credentialCheckSuccessIps.add(
                        Tuple.of(id, credential, ip, result.getString(Fields.DiscoveryResult.MESSAGE), Fields.DiscoveryResult.COMPLETED_STATUS));
            }
            else
            {
                credentialCheckFailedIps.add(
                        Tuple.of(id, null, ip, result.getString(Fields.DiscoveryResult.MESSAGE), Fields.DiscoveryResult.FAILED_STATUS));
            }
        }

        // Batch insert results, handling empty lists to avoid batch query errors
        Future<JsonArray> failureIps = credentialCheckFailedIps.isEmpty()
                ? Future.succeededFuture(new JsonArray())
                : DbUtils.sendQueryExecutionRequest(Queries.Discovery.INSERT_RESULT, credentialCheckFailedIps);

        Future<JsonArray> successIps = credentialCheckSuccessIps.isEmpty()
                ? Future.succeededFuture(new JsonArray())
                : DbUtils.sendQueryExecutionRequest(Queries.Discovery.INSERT_RESULT, credentialCheckSuccessIps);

        return Future.join(failureIps, successIps)
                .compose(v -> Future.succeededFuture());
    }

    private JsonArray getIpsFromString(String ip, String ipType)
    {
        var ips = new JsonArray();

        if (Validators.validateIpWithIpType(ip, ipType))
        {
            return ips;
        }

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

    private Future<JsonArray> pingIps(JsonArray ips)
    {
        // Early validation - outside executeBlocking
        if (ips == null || ips.isEmpty())
        {
            return Future.succeededFuture(new JsonArray());
        }

        LOGGER.debug("ping check request for ips: " + ips.encode());

        // Prepare fping command - outside executeBlocking
        var command = new String[ips.size() + 3];
        command[0] = "fping";
        command[1] = "-c1";
        command[2] = "-q";

        for (var i = 0; i < ips.size(); i++)
        {
            command[i + 3] = ips.getString(i);
        }

        // execute the blocking ping operation within executeBlocking
        return App.VERTX.executeBlocking(promise ->
        {
            var results = new JsonArray();

            Process process = null;

            BufferedReader reader = null;

            try
            {
                var processBuilder = new ProcessBuilder(command);

                processBuilder.redirectErrorStream(true);

                process = processBuilder.start();

                // Process timeout
                var completed = process.waitFor(Config.BASE_TIME + (Config.DISCOVERY_TIMEOUT_PER_IP * ips.size()), TimeUnit.SECONDS);

                if (!completed)
                {
                    process.destroyForcibly();

                    promise.complete(createErrorResultForAll(ips, "Ping process timed out"));

                    return;
                }

                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;

                var processedIps = new JsonArray();

                while ((line = reader.readLine()) != null)
                {
                    var parts = line.split(":");

                    if (parts.length < 2)
                    {
                        continue;
                    }

                    var ip = parts[0].trim();

                    var stats = parts[1].trim();

                    processedIps.add(ip);

                    var isSuccess = !stats.contains("100%");

                    var result = new JsonObject()
                            .put(SUCCESS, isSuccess)
                            .put(IP, ip)
                            .put(MESSAGE,
                                    isSuccess
                                            ? "Ping check success"
                                            : "Ping check failed: 100% packet loss");

                    results.add(result);
                }

                // Handle unprocessed IPs
                for (var i = 0; i < ips.size(); i++)
                {
                    var ip = ips.getString(i);

                    if (!processedIps.contains(ip))
                    {
                        results.add(createErrorResult(ip, "No response from fping"));
                    }
                }

                LOGGER.debug("ping check passed ips: " + results.encode());

                promise.complete(results);
            }
            catch (Exception exception)
            {
                LOGGER.error("Error during ping check: " + exception.getMessage());

                promise.complete(createErrorResultForAll(ips, "Error during ping check"));
            }
            finally
            {
                closeQuietly(reader);

                if (process != null && process.isAlive())
                {
                    try
                    {
                        process.destroyForcibly();
                    }
                    catch (Exception exception)
                    {
                        LOGGER.error("Error destroying ping process: " + exception.getMessage());
                    }
                }
            }
        });
    }

    private Future<JsonArray> checkPorts(JsonArray ips, int port)
    {
        var promise = Promise.<JsonArray>promise();

        var results = new JsonArray();

        var futures = new ArrayList<Future<JsonObject>>();

        if (ips == null || ips.isEmpty() || port < 1 || port > 65535)
        {
            promise.complete(results);

            return promise.future();
        }

        LOGGER.debug("port check request for ips: " + ips.encode());

        for (var ip : ips)
        {
            var checkPortPromise = Promise.<JsonObject>promise();

            futures.add(checkPortPromise.future());

            var result = new JsonObject()
                    .put(IP, ip.toString())
                    .put(PORT, port);

            try
            {
                App.VERTX.createNetClient(new NetClientOptions().setConnectTimeout(Config.PORT_TIMEOUT * 1000))
                        .connect(port, ip.toString(), asyncResult ->
                        {
                            try
                            {
                                if (asyncResult.succeeded())
                                {
                                    var socket = asyncResult.result();

                                    result.put(SUCCESS, true)
                                            .put(MESSAGE, "Port " + port + " is open on " + ip);

                                    socket.close();
                                }
                                else
                                {
                                    var cause = asyncResult.cause();

                                    var errorMessage = cause != null ? cause.getMessage() : "Unknown error";

                                    result.put(SUCCESS, false)
                                            .put(MESSAGE,
                                                    errorMessage.contains("Connection refused")
                                                            ? "Port " + port + " is closed on " + ip
                                                            : errorMessage);
                                }
                            }
                            catch (Exception exception)
                            {
                                result
                                        .put(SUCCESS, false)
                                        .put(MESSAGE, "Something went wrong");

                                LOGGER.error("Something went wrong, error: " + exception.getMessage());
                            }
                            finally
                            {
                                results.add(result);

                                checkPortPromise.complete(result);
                            }
                        });
            }
            catch (Exception exception)
            {
                result
                        .put(SUCCESS, false)
                        .put(MESSAGE, "Error creating connection: " + exception.getMessage());
                results
                        .add(result);

                checkPortPromise.complete(result);
            }
        }

        Future.all(futures)
                .onComplete(asyncResult ->
                {
                    if (asyncResult.succeeded())
                    {
                        LOGGER.debug("Port check passed ips: " + results.encode());

                        promise.complete(results);
                    }
                    else
                    {
                        LOGGER.error("Error in port check: " + asyncResult.cause().getMessage());

                        promise.complete(results);
                    }
                });

        return promise.future();
    }

    private JsonObject createErrorResult(String ip, String message)
    {
        return new JsonObject()
                .put(Fields.Discovery.IP, ip)
                .put(Fields.Discovery.SUCCESS, false)
                .put(Fields.DiscoveryResult.MESSAGE, message);
    }

    private JsonArray createErrorResultForAll(JsonArray ipArray, String message)
    {
        var results = new JsonArray();

        if (ipArray != null)
        {
            for (var i = 0; i < ipArray.size(); i++)
            {
                var ip = String.valueOf(ipArray.getValue(i));

                results.add(createErrorResult(ip, message));
            }
        }
        return results;
    }

    private void closeQuietly(AutoCloseable closeable)
    {
        if (closeable != null)
        {
            try
            {
                closeable.close();
            }
            catch (Exception exception)
            {
                LOGGER.error("Error closing resource: " + exception.getMessage());
            }
        }
    }

}