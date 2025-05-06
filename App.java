package org.nms;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.nms.Database.Models.*;
import org.nms.API.Server;
import org.nms.Scheduler.Scheduler;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class App
{
    public static Vertx vertx = Vertx.vertx(new VertxOptions().setMaxWorkerExecuteTime(300).setMaxWorkerExecuteTimeUnit(TimeUnit.SECONDS));

    public static final UserModel userModel = UserModel.getInstance();

    public static final CredentialModel credentialModel = CredentialModel.getInstance();

    public static final DiscoveryModel discoveryModel = DiscoveryModel.getInstance();

    public static final ProvisionModel provisionModel = ProvisionModel.getInstance();

    public static final MetricGroupModel metricGroupModel = MetricGroupModel.getInstance();

    public static final MetricResultModel metricResultModel = MetricResultModel.getInstance();

    public static Future<Void> createUserSchemaFuture = userModel.createSchema();

    public static Future<Void> createCredentialSchemaFuture = credentialModel.createSchema();

    public static Future<Void> createDiscoverySchemaFuture = discoveryModel.createSchema();

    public static Future<Void> createProvisionSchemaFuture = provisionModel.createSchema();

    public static Future<Void> createMetricGroupSchemaFuture = metricGroupModel.createSchema();

    public static Future<Void> createPolledDataSchemaFuture = metricResultModel.createSchema();


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
        )).onSuccess(v -> vertx.deployVerticle(new Server(), httpResult ->
        {
            if (httpResult.succeeded())
            {
                scheduler.start();

                ConsoleLogger.info("Success Deploying HttpVerticle On Thread [ " + Thread.currentThread().getName() + " ] ID [ " + httpResult.result() + " ]");
            }
            else
            {
                scheduler.stop();

                ConsoleLogger.error("Failure Deploying HTTP Verticle, Cause [ " + httpResult.cause().getMessage() + " ]");
            }
        })).onFailure(err ->
        {
            ConsoleLogger.error("Failed to create schemas " + err.getMessage());

            scheduler.stop();
        });

    }
}
