package org.nms.discovery;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nms.constants.Database;
import org.nms.constants.Eventbus;
import org.nms.utils.ApiUtils;
import org.nms.utils.DbUtils;

import static org.nms.App.LOGGER;

public class Discovery extends AbstractVerticle
{
    @Override
    public void start(Promise startPromise)
    {
        vertx.deployVerticle(new PingService())
                        .compose(v -> vertx.deployVerticle(new PortService()))
                        .compose(v -> vertx.deployVerticle(new CredentialCheckService()))
                        .compose(v ->
                        {
                            vertx.eventBus().localConsumer(Eventbus.RUN_DISCOVERY, this::runDiscovery);

                            startPromise.complete();

                            LOGGER.info("Discovery Verticle Started");

                            return Future.succeededFuture();
                        });

    }

    @Override
    public void stop()
    {
        LOGGER.info("Discovery Verticle Stopped");
    }

    public void runDiscovery(Message<JsonObject> message)
    {
        var discoveryId = message.body().getInteger(Database.Discovery.ID);

        var queryRequest = DbUtils.buildRequest(
                Database.Table.DISCOVERY_PROFILE,
                Database.Operation.GET,
                new JsonArray()
                        .add(new JsonObject()
                        .put(Database.Common.COLUMN, Database.Discovery.ID)
                        .put(Database.Common.VALUE, discoveryId)),
                null,
                null
        );

        vertx.eventBus().<JsonArray>request(Eventbus.EXECUTE_QUERY, queryRequest, dbResponse ->
        {
            if(dbResponse.succeeded())
            {
                var discovery = dbResponse.result().body().getJsonObject(0);

                vertx.eventBus().send(Eventbus.PING_CHECK_START, discovery);
            }
            else
            {
                LOGGER.warn("Discovery with id " + discoveryId + " not found.");
            }
        });
    }
}
