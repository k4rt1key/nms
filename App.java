package org.nms;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.nms.database.Database;

import java.util.concurrent.TimeUnit;

import org.nms.api.Server;
import org.nms.discovery.Discovery;
import org.nms.plugin.Plugin;
import org.nms.scheduler.Scheduler;

public class App
{
    public static Vertx vertx = Vertx.vertx(new VertxOptions().setMaxWorkerExecuteTime(300).setMaxWorkerExecuteTimeUnit(TimeUnit.SECONDS));

    public static void main( String[] args )
    {

        try
        {
            // ===== Start DB Verticle =====
            vertx.deployVerticle(new Database())
                    // ===== Start Scheduler =====
                    .compose(v -> vertx.deployVerticle(new Scheduler()))
                    // ===== Deploy Plugin Verticle =====
                    .compose(v -> vertx.deployVerticle(new Plugin()))
                    // ===== Deploy Discovery Verticle ======
                    .compose(v -> vertx.deployVerticle(new Discovery()))
                    // ===== Start Http Server =====
                    .compose(v -> vertx.deployVerticle(new Server()))
                    // ===== Success =====
                    .onSuccess(v -> Logger.info("✅ Successfully Started NMS Application"))
                    // ===== Failure =====
                    .onFailure(err -> Logger.error("❌ Failed to start NMS Application " + err.getMessage()));
        }
        catch (Exception e)
        {
            Logger.error("❌ Error Starting Application");
        }
    }
}
