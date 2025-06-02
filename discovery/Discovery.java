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
import org.nms.utils.DiscoveryUtils;

import static org.nms.App.LOGGER;

public class Discovery extends AbstractVerticle
{
    @Override
    public void start()
    {
        vertx.eventBus().localConsumer(Eventbus.RUN_DISCOVERY, this::runDiscovery);

        LOGGER.info("Discovery Verticle Started");
    }

    @Override
    public void stop()
    {
        LOGGER.info("Discovery Verticle Stopped");
    }

    public void runDiscovery(Message<JsonObject> message)
    {
        var discoveryId = message.body().getInteger(Database.Discovery.ID);


        DiscoveryUtils.pingIps(message);

        DiscoveryUtils.portCheck(message);



    }

}
