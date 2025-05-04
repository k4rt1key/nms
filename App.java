package org.nms;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.nms.Database.Services.*;
import org.nms.HttpServer.Server;
import org.nms.Scheduler.Scheduler;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class App
{
    public static Vertx vertx = Vertx.vertx(new VertxOptions().setMaxWorkerExecuteTime(300).setMaxWorkerExecuteTimeUnit(TimeUnit.SECONDS));

    public static final UserService userService = UserService.getInstance();

    public static final CredentialService credentialService = CredentialService.getInstance();

    public static final DiscoveryService discoveryService = DiscoveryService.getInstance();

    public static final ProvisionService provisionService = ProvisionService.getInstance();

    public static final MetricGroupService metricGroupService = MetricGroupService.getInstance();

    public static final PolledDataService polledDataService = PolledDataService.getInstance();

    // Initialize Database
    public static Future<Void> createUserSchemaFuture = userService.createSchema();

    public static Future<Void> createCredentialSchemaFuture = credentialService.createSchema();

    public static Future<Void> createDiscoverySchemaFuture = discoveryService.createSchema();

    public static Future<Void> createProvisionSchemaFuture = provisionService.createSchema();

    public static Future<Void> createMetricGroupSchemaFuture = metricGroupService.createSchema();

    public static Future<Void> createPolledDataSchemaFuture = polledDataService.createSchema();


    public static void main( String[] args )
    {

        Scheduler scheduler = new Scheduler(vertx, 10);

        Future.join(List.of(
                createUserSchemaFuture,
                createCredentialSchemaFuture,
                createDiscoverySchemaFuture,
                createProvisionSchemaFuture,
                createMetricGroupSchemaFuture,
                createPolledDataSchemaFuture
        )).onSuccess(v -> {

            vertx.deployVerticle(new Server(), httpResult -> {
                if (httpResult.succeeded()) {
                    scheduler.start();
                    ConsoleLogger.info("Success Deploying HttpVerticle On Thread [ " + Thread.currentThread().getName() + " ] ID [ " + httpResult.result() + " ]");
                } else {
                    scheduler.stop();
                    ConsoleLogger.error("Failure Deploying HTTP Verticle, Cause [ " + httpResult.cause().getMessage() + " ]");
                }
            });
        }).onFailure(err -> {
            ConsoleLogger.error("Schema creation failed: " + err.getMessage());
            scheduler.stop();
            err.printStackTrace(); // Log stack trace too for full info
        });

    }
}
