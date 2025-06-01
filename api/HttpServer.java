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

import org.nms.constants.Global;

public class HttpServer extends AbstractVerticle
{
    private static final JWTAuthOptions config = new JWTAuthOptions()
                                                    .setKeyStore(new KeyStoreOptions().setPath(Global.KEYSTORE_PATH).setPassword(Global.JWT_SECRET));

    public static final JWTAuth jwtAuth = JWTAuth.create(App.VERTX, config);

    public static final JWTAuthHandler jwtAuthHandler = JWTAuthHandler.create(jwtAuth);

    @Override
    public void start(Promise<Void> startPromise)
    {
        var server = vertx.createHttpServer(new HttpServerOptions().setReuseAddress(true));

        var router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        router.route().handler(ctx ->
        {
            ctx.response().putHeader("Content-Type", "application/json");
            ctx.next();
        });

        User.getInstance().init(router);

        router.route()
                .handler(jwtAuthHandler)
                .handler(HttpServer::authenticate);


        Credential.getInstance().init(router);

        Discovery.getInstance().init(router);

        Monitor.getInstance().init(router);

        PollingResult.getInstance().init(router);

        server.requestHandler(router);

        server.listen(Global.HTTP_PORT, http ->
        {
            if(http.succeeded())
            {
                LOGGER.info("✅ HTTP Server Started On Port => " + Global.HTTP_PORT + " On Thread [ " + Thread.currentThread().getName() + " ] ");

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
        LOGGER.info("Http Server Stopped");
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