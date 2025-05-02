package org.nms;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.nms.Database.Services.*;
import org.nms.HttpServer.Server;

import java.util.List;

public class App
{
    public static Vertx vertx = Vertx.vertx();

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
        Future.join(
                List.of(
                       createUserSchemaFuture,
                        createCredentialSchemaFuture,
                        createDiscoverySchemaFuture,
                        createProvisionSchemaFuture,
                        createMetricGroupSchemaFuture,
                        createPolledDataSchemaFuture
                )
        );

        vertx.deployVerticle(new Server(), httpResult ->
        {
            if (httpResult.succeeded())
            {
                ConsoleLogger.info("Success Deploying HttpVerticle On Thread [ " + Thread.currentThread().getName() + " ] ID [ " + httpResult.result() + " ]");
            }
            else
            {
                ConsoleLogger.error("Failure Deploying HTTP Verticle, Cause [ " + httpResult.cause().getMessage() + " ]");
            }
        });

    }
}
