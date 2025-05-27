package org.nms.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;

import org.nms.App;
import static org.nms.App.LOGGER;
import static org.nms.utils.ApiUtils.sendFailure;

import org.nms.constants.Config;

public class HttpServer extends AbstractVerticle
{
    private static final JWTAuthOptions config = new JWTAuthOptions()
                                                    .setKeyStore(new KeyStoreOptions().setPath(Config.KEYSTORE_PATH).setPassword(Config.JWT_SECRET));

    public static final JWTAuth jwtAuth = JWTAuth.create(App.VERTX, config);

    public static final JWTAuthHandler jwtAuthHandler = JWTAuthHandler.create(jwtAuth);

    @Override
    public void start(Promise<Void> startPromise)
    {
        // ===== Configure HttpServer =====
        var server = vertx.createHttpServer(new HttpServerOptions().setReuseAddress(true));

        // ===== Configure Routes ======
        var router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        router.route().handler(ctx ->
        {
            ctx.response().putHeader("Content-Type", "application/json");
            ctx.next();
        });

        // ===== Configure User Routes =====
        User.getInstance().init(router);

        router.route()
                .handler(jwtAuthHandler)
                .handler(HttpServer::authenticate);


        // ===== Configure Credential Routes =====
        Credential.getInstance().init(router);

        // ===== Configure Discovery Routes =====
        Discovery.getInstance().init(router);

        // ===== Configure Provision Routes =====
        Provision.getInstance().init(router);

        // ===== Configure Result Routes =====
        Result.getInstance().init(router);

        // ===== Configure Router To Server =====
        server.requestHandler(router);

        // ===== Listen on Port =====
        server.listen(Config.HTTP_PORT, http ->
        {
            if(http.succeeded())
            {
                LOGGER.info("✅ HTTP Server Started On Port => " + Config.HTTP_PORT + " On Thread [ " + Thread.currentThread().getName() + " ] ");

                startPromise.complete();
            }
            else
            {
                LOGGER.error("Failed To Start HTTP Server => " + http.cause());

                startPromise.fail(http.cause());
            }
        });
    }

    @Override
    public void stop()
    {
        LOGGER.info("\uD83D\uDED1 Http Server Stopped");
    }

    public static void authenticate(RoutingContext ctx)
    {
        if(ctx.user() == null || ctx.user().principal().isEmpty() || ctx.user().expired())
        {
            sendFailure(ctx, 401, "Please login to continue");
            return;
        }

        ctx.next();
    }
}