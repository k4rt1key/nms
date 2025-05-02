package org.nms.HttpServer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import org.nms.App;
import org.nms.ConsoleLogger;
import org.nms.Database.Services.*;
import org.nms.HttpServer.Middlewares.AuthMiddleware;
import org.nms.HttpServer.Routers.*;

import java.util.List;

public class Server extends AbstractVerticle
{

    public static final String CREDENTIALS_ENDPOINT = "/api/v1/credential/*";

    public static final String DISCOVERY_ENDPOINT = "/api/v1/discovery/*";

    public static final String PROVISION_ENDPOINT = "/api/v1/provision/*";

    public static final String POLLING_ENDPOINT = "/api/v1/polling/*";

    public static final String USER_ENDPOINT = "/api/v1/user/*";

    public static final int HTTP_PORT = 8080;

    @Override
    public void start(Promise<Void> startPromise)
    {

        var server = App.vertx.createHttpServer(new HttpServerOptions().setReuseAddress(true));

        var router = Router.router(App.vertx);

        router.route().handler(BodyHandler.create());

        router.route().handler(ctx ->
        {
            ctx.response().putHeader("Content-Type", "application/json");
            ctx.next();
        });

        router.route(USER_ENDPOINT).subRouter(UserRouter.router());

        router.route()
                .handler(JWTAuthHandler.create(JwtConfig.jwtAuth))
                .handler(AuthMiddleware::authenticate);

        router.route(CREDENTIALS_ENDPOINT).subRouter(CredentialRouter.router());

        router.route(DISCOVERY_ENDPOINT).subRouter(DiscoveryRouter.router());

        router.route(PROVISION_ENDPOINT).subRouter(ProvisionRouter.router());

        router.route(POLLING_ENDPOINT).subRouter(PollingRouter.router());

        server.requestHandler(router);


        server.listen(HTTP_PORT, http -> {
            if(http.succeeded())
            {
                ConsoleLogger.info("HTTP Server Started On Port => " + HTTP_PORT);
                startPromise.complete();
            }
            else
            {
                ConsoleLogger.error("Failed To Start HTTP Server => " + http.cause());
                startPromise.fail(http.cause());
            }
        });

    }

    @Override
    public void stop()
    {
        ConsoleLogger.info("Http Server Stopped");
    }
}