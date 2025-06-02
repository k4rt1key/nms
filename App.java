package org.nms;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.concurrent.TimeUnit;

import org.nms.api.HttpServer;
import org.nms.constants.Configuration;
import org.nms.database.Database;
import org.nms.discovery.Discovery;
import org.nms.plugin.Plugin;
import org.nms.scheduler.Scheduler;


public class App
{
    public static final Vertx VERTX = Vertx.vertx(new VertxOptions().setMaxWorkerExecuteTime(Configuration.MAX_WORKER_EXECUTE_TIME).setMaxWorkerExecuteTimeUnit(TimeUnit.SECONDS));

    public static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args)
    {

        Runtime.getRuntime().addShutdownHook(new Thread(VERTX::close));

        try
        {
            VERTX.deployVerticle(new Database())
                    .compose(v -> VERTX.deployVerticle(new Scheduler()))
                    .compose(v -> VERTX.deployVerticle(new Plugin()))
                    .compose(v -> VERTX.deployVerticle(new Discovery()))
                    .compose(v -> VERTX.deployVerticle(new HttpServer()))

            .onComplete(asyncResult ->
            {
                if (asyncResult.succeeded())
                {
                    LOGGER.info("✅ Successfully Started NMS Application");
                }
                else
                {
                    LOGGER.error("❌ Failed to start NMS Application, cause => " + asyncResult.cause().getMessage());
                }
            });
        }
        catch (Exception exception)
        {
            LOGGER.error("❌ Failed to start NMS Application, cause => " + exception.getMessage());
        }
    }
}