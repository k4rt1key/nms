package org.nms.discovery;

import inet.ipaddr.IPAddressString;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.nms.App.LOGGER;
import static org.nms.constants.Fields.Discovery.*;
import static org.nms.constants.Fields.DiscoveryResult.MESSAGE;

public class Discovery extends AbstractVerticle
{

    @Override
    public void start()
    {
        vertx.eventBus().<JsonObject>localConsumer(Fields.EventBus.RUN_DISCOVERY_ADDRESS, message ->
        {
            int id = message.body().getInteger(ID);

            runDiscoveryProcess(id).onFailure(err ->
                    LOGGER.error("Discovery failed for id " + id + ": " + err.getMessage()));

        });

        LOGGER.debug("✅ Discovery Verticle Deployed on thread [" + Thread.currentThread().getName() + "]");
    }

    @Override
    public void stop()
    {
        LOGGER.info("🛑 Discovery Verticle Stopped");
    }

    private Future<Void> runDiscoveryProcess(int id)
    {
        return fetchDiscoveryDetails(id)

                .compose(discovery ->

                         updateDiscoveryStatus(id, RUNNING_STATUS)

                        .compose(v -> DbUtils.sendQueryExecutionRequest(Queries.Discovery.DELETE_RESULT, new JsonArray().add(id)))

                        .compose(v -> executeDiscovery(id, discovery)))

                .compose(v -> updateDiscoveryStatus(id, COMPLETED_STATUS))

                .recover(err ->
                {
                    LOGGER.warn("Discovery failed: " + err.getMessage());

                    return Future.failedFuture(err);
                });
    }

    private Future<Void> executeDiscovery(int id, JsonObject discovery)
    {
        var ips = getIpsFromString(discovery.getString(IP), discovery.getString(IP_TYPE));

        var port = discovery.getInteger(PORT);

        var credentials = discovery.getJsonArray(CREDENTIAL_JSON);

        return pingIps(ips)

                .compose(results -> insertResults(id, results, false))

                .compose(passedIps -> checkPorts(passedIps, port))

                .compose(results -> insertResults(id, results, false))

                .compose(passedIps -> passedIps.isEmpty() ?
                        Future.succeededFuture() :
                        sendDiscoveryRequest(id, passedIps, port, credentials)
                                .compose(results -> processCredentialResults(id, results)));
    }

    private Future<JsonArray> sendDiscoveryRequest(int id, JsonArray ips, int port, JsonArray credentials)
    {
        var request = new JsonObject()
                .put(Fields.PluginDiscoveryRequest.TYPE, Fields.PluginDiscoveryRequest.DISCOVERY)
                .put(Fields.PluginDiscoveryRequest.ID, id)
                .put(Fields.PluginDiscoveryRequest.IPS, ips)
                .put(Fields.PluginDiscoveryRequest.PORT, port)
                .put(Fields.PluginDiscoveryRequest.CREDENTIALS, credentials);

        return vertx.eventBus().<JsonArray>request(Fields.EventBus.PLUGIN_SPAWN_ADDRESS, request)

                .map(Message::body)

                .recover(err ->
                {
                    LOGGER.error("Plugin error: " + err.getMessage());

                    return Future.succeededFuture(new JsonArray());
                });
    }

    private Future<JsonObject> fetchDiscoveryDetails(int id)
    {
        return DbUtils.sendQueryExecutionRequest(Queries.Discovery.GET_BY_ID, new JsonArray().add(id))

                .compose(results -> results.isEmpty() ?

                        Future.failedFuture("Discovery not found") :

                        Future.succeededFuture(results.getJsonObject(0)));
    }

    private Future<Void> updateDiscoveryStatus(int id, String status)
    {
        return DbUtils.sendQueryExecutionRequest(Queries.Discovery.UPDATE_STATUS, new JsonArray().add(id).add(status))
                .mapEmpty();
    }

    private Future<JsonArray> insertResults(int id, JsonArray results, boolean isCredentialCheck)
    {
        var passedIps = new JsonArray();

        var failedTuples = new ArrayList<Tuple>();

        results.forEach(item ->
        {
            var result = (JsonObject) item;

            var success = result.getBoolean(SUCCESS);

            var ip = result.getString(IP);

            if (success)
            {
                passedIps.add(ip);
            }
            else
            {
                var credentialId = isCredentialCheck && result.containsKey(Fields.PluginDiscoveryResponse.CREDENTIALS) ?

                        result.getJsonObject(Fields.PluginDiscoveryResponse.CREDENTIALS).getInteger(Fields.Credential.ID) : null;

                failedTuples.add(
                        Tuple.of(
                                id,
                                credentialId,
                                ip,
                                result.getString(MESSAGE),
                                 FAILED_STATUS,
                                OffsetDateTime.now(ZoneId.of("Asia/Kolkata")).toString()
                        )
                );
            }
        });

        return failedTuples.isEmpty() ?

                Future.succeededFuture(passedIps) :

                DbUtils.sendQueryExecutionRequest(Queries.Discovery.INSERT_RESULT, failedTuples)

                        .map(v -> passedIps);
    }

    private Future<Void> processCredentialResults(int id, JsonArray results)
    {
        var successTuples = new ArrayList<Tuple>();

        var failedTuples = new ArrayList<Tuple>();

        results.forEach(item ->
        {
            var result = (JsonObject) item;
            var success = result.getBoolean(SUCCESS);
            var ip = result.getString(Fields.PluginDiscoveryResponse.IP);
            var message = result.getString(MESSAGE);
            var time = result.getString(Fields.DiscoveryResult.TIME);

            if (success)
            {
                var credentialId = result.getJsonObject(Fields.PluginDiscoveryResponse.CREDENTIALS)
                        .getInteger(Fields.Credential.ID);
                successTuples.add(Tuple.of(id, credentialId, ip, message,
                        Fields.DiscoveryResult.COMPLETED_STATUS, time));
            } else
            {
                failedTuples.add(Tuple.of(id, null, ip, message,
                        Fields.DiscoveryResult.FAILED_STATUS, time));
            }
        });

        var futures = new ArrayList<Future<JsonArray>>();

        if (!failedTuples.isEmpty())
        {
            futures.add(DbUtils.sendQueryExecutionRequest(Queries.Discovery.INSERT_RESULT, failedTuples));
        }

        if (!successTuples.isEmpty())
        {
            futures.add(DbUtils.sendQueryExecutionRequest(Queries.Discovery.INSERT_RESULT, successTuples));
        }

        return futures.isEmpty() ? Future.succeededFuture() :
                Future.join(futures).mapEmpty();
    }

    private JsonArray getIpsFromString(String ip, String ipType)
    {
        var ips = new JsonArray();

        if (Validators.validateIpWithIpType(ip, ipType)) return ips;

        try
        {
            switch (ipType.toUpperCase())
            {
                case "RANGE":

                    var parts = ip.split("-");

                    var startIp = new IPAddressString(parts[0].trim()).getAddress();

                    var endIp = new IPAddressString(parts[1].trim()).getAddress();

                    startIp.toSequentialRange(endIp).getIterable()
                            .forEach(addr -> ips.add(addr.toString()));

                    break;

                case "CIDR":

                    new IPAddressString(ip).getSequentialRange().getIterable()
                            .forEach(addr -> ips.add(addr.toString()));

                    break;

                case "SINGLE":

                    ips.add(new IPAddressString(ip).getAddress().toString());

                    break;
            }
        }
        catch (Exception e)
        {
            LOGGER.error("IP conversion failed: " + e.getMessage());
        }

        return ips;
    }

    private Future<JsonArray> pingIps(JsonArray ips)
    {
        if (ips == null || ips.isEmpty()) return Future.succeededFuture(new JsonArray());

        var command = new String[ips.size() + 3];

        command[0] = "fping";

        command[1] = "-c1";

        command[2] = "-q";

        for (int i = 0; i < ips.size(); i++)
        {
            command[i + 3] = ips.getString(i);
        }

        return App.VERTX.executeBlocking(promise ->
        {
            var results = new JsonArray();

            Process process = null;

            BufferedReader reader = null;

            try
            {
                process = new ProcessBuilder(command).redirectErrorStream(true).start();

                boolean completed = process.waitFor(Config.BASE_TIME +
                        ((long) Config.DISCOVERY_TIMEOUT_PER_IP * ips.size()), TimeUnit.SECONDS);

                if (!completed)
                {
                    process.destroyForcibly();

                    promise.complete(createErrorResults(ips, "Ping timeout"));

                    return;
                }

                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                var processedIps = new JsonArray();

                String line;

                while ((line = reader.readLine()) != null)
                {
                    var parts = line.split(":");

                    if (parts.length < 2) continue;

                    var ip = parts[0].trim();

                    var stats = parts[1].trim();

                    processedIps.add(ip);

                    boolean isSuccess = !stats.contains("100%");

                    results.add(new JsonObject()
                            .put(SUCCESS, isSuccess)
                            .put(IP, ip)
                            .put(MESSAGE, isSuccess ? "Ping success" : "Ping failed: 100% packet loss"));
                }

                // Add unprocessed IPs as failures
                ips.forEach(ip ->
                {
                    if (!processedIps.contains(ip))
                    {
                        results.add(new JsonObject().put(IP, ip.toString()).put(SUCCESS, false).put(MESSAGE, "No fping result"));
                    }
                });

                promise.complete(results);
            }
            catch (Exception e)
            {
                LOGGER.error("Ping error: " + e.getMessage());

                promise.complete(createErrorResults(ips, "Ping error"));
            }
            finally
            {
                if (reader != null)
                {
                    try
                    {
                        reader.close();
                    }
                    catch (Exception e)
                    {
                        LOGGER.error("Resource close error: " + e.getMessage());
                    }
                }

                if (process != null && process.isAlive())
                {
                    process.destroyForcibly();
                }
            }
        });
    }

    private Future<JsonArray> checkPorts(JsonArray ips, int port)
    {
        if (ips == null || ips.isEmpty() || port < 1 || port > 65535)
        {
            return Future.succeededFuture(new JsonArray());
        }

        var results = new JsonArray();

        var futures = new ArrayList<Future<JsonObject>>();

        ips.forEach(ip ->
        {
            var promise = Promise.<JsonObject>promise();

            futures.add(promise.future());

            var result = new JsonObject().put(IP, ip.toString()).put(PORT, port);

            App.VERTX.createNetClient(new NetClientOptions().setConnectTimeout(Config.PORT_TIMEOUT * 1000))
                    .connect(port, ip.toString(), ar ->
                    {
                        if (ar.succeeded())
                        {
                            ar.result().close();

                            result.put(SUCCESS, true).put(MESSAGE, "Port " + port + " open on " + ip);

                        }
                        else
                        {
                            var error = ar.cause().getMessage();

                            result.put(SUCCESS, false).put(MESSAGE,
                                    error.contains("Connection refused") ?
                                            "Port " + port + " closed on " + ip : error);
                        }

                        results.add(result);

                        promise.complete(result);
                    });
        });

        return Future.all(futures).map(v -> results);
    }

    private JsonArray createErrorResults(JsonArray ips, String message)
    {
        var results = new JsonArray();

        if (ips != null)
        {
            ips.forEach(ip -> results.add(
                    new JsonObject()
                            .put(IP, ip.toString())
                            .put(SUCCESS, false)
                            .put(MESSAGE, message)
            ));
        }

        return results;
    }

}