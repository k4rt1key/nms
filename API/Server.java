package org.nms.API;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import org.nms.App;
import org.nms.ConsoleLogger;
import org.nms.API.RequestHandlers.*;
import org.nms.API.Middlewares.AuthMiddleware;
import org.nms.API.Middlewares.Validators.CredentialRequestValidator;
import org.nms.API.Middlewares.Validators.DiscoveryRequestValidator;
import org.nms.API.Middlewares.Validators.ProvisionRequestValidator;
import org.nms.API.Middlewares.Validators.UserRequestValidator;

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


        server.listen(HTTP_PORT, http ->
        {
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

class UserRouter
{

    public static Router router()
    {
        var router = Router.router(App.vertx);

        /**
         * GET /api/v1/user/
         */
        router.get("/")
                .handler(UserHandler::getUsers);


        /**
         * GET  /api/v1/user/:id
         */
        router.get("/:id")
                .handler(JWTAuthHandler.create(JwtConfig.jwtAuth))
                .handler(AuthMiddleware::authenticate)
                .handler(UserRequestValidator::getUserByIdRequestValidator)
                .handler(UserHandler::getUserById);

        /**
         * POST /api/v1/user/login
         */
        router.post("/login")
                .handler(UserRequestValidator::loginRequestValidator)
                .handler(UserHandler::login);

        /**
         * POST /api/v1/user/register
         */
        router.post("/register")
                .handler(UserRequestValidator::registerRequestValidator)
                .handler(UserHandler::register);

        /**
         * PATCH /api/v1/user/:id
         */
        router.patch("/:id")
                .handler(JWTAuthHandler.create(JwtConfig.jwtAuth))
                .handler(AuthMiddleware::authenticate)
                .handler(UserRequestValidator::updateUserRequestValidator)
                .handler(UserHandler::updateUser);

        /**
         * DELETE /api/v1/user/:id
         */
        router.delete("/:id")
                .handler(JWTAuthHandler.create(JwtConfig.jwtAuth))
                .handler(AuthMiddleware::authenticate)
                .handler(UserRequestValidator::deleteUserRequestValidator)
                .handler(UserHandler::deleteUser);

        return router;
    }
}

class ProvisionRouter
{
    public static Router router()
    {
        var router = Router.router(App.vertx);

        /**
         * GET  /api/v1/provision
         */
        router.get("/")
                .handler(JWTAuthHandler.create(JwtConfig.jwtAuth))
                .handler(ProvisionHandler::getAllProvisions);

        /**
         * GET  /api/v1/provision/:id
         */
        router.get("/:id")
                .handler(ProvisionRequestValidator::getProvisionByIdRequestValidator)
                .handler(ProvisionHandler::getProvisionById);

        /**
         * POST /api/v1/provision
         */
        router.post("/")
                .handler(ProvisionRequestValidator::createProvisionRequestValidator)
                .handler(ProvisionHandler::createProvision);


        /**
         * PATCH /api/v1/provisions/:id
         *
         * To Update Provisioned device's metric data such as
         * - Add metric to poll
         * - Remove metric to poll
         * - Change polling interval of metric
         */
        router.patch("/:id")
                .handler(ProvisionRequestValidator::updateProvisionRequestValidator)
                .handler(ProvisionHandler::updateMetric);

        /**
         * DELETE /api/v1/provision/:id
         */
        router.delete("/:id")
                .handler(ProvisionRequestValidator::deleteProvisionRequestValidator)
                .handler(ProvisionHandler::deleteProvision);


        return router;
    }
}

class DiscoveryRouter
{
    public static Router router()
    {
        var router = Router.router(App.vertx);

        /**
         * GET  /api/v1/discovery/results/:id
         */

        router.get("/results/:id")
                .handler(DiscoveryRequestValidator::getDiscoveryByIdRequestValidator)
                .handler(DiscoveryHandler::getDiscoveryResultsById);

        /**
         * GET  /api/v1/discovery/results
         */

        router.get("/results")
                .handler(DiscoveryHandler::getDiscoveryResults);

        /**
         * GET  /api/v1/discovery
         */
        router.get("/")
                .handler(DiscoveryHandler::getAllDiscoveries);

        /**
         * GET  /api/v1/discovery/:id
         */
        router.get("/:id")
                .handler(DiscoveryRequestValidator::getDiscoveryByIdRequestValidator)
                .handler(DiscoveryHandler::getDiscoveryById);


        /**
         * POST /api/v1/discoveries
         *
         * To create Discovery_profile
         */
        router.post("/")
                .handler(DiscoveryRequestValidator::createDiscoveryRequestValidator)
                .handler(DiscoveryHandler::createDiscovery);


        /**
         * POST /api/v1/discovery/run/:id
         *
         * To run Discovery
         */
        router.post("/run/:id")
                .handler(DiscoveryRequestValidator::runDiscoveryRequestValidator)
                .handler(DiscoveryHandler::runDiscovery);

        /**
         * PATCH /api/v1/discovery/:id
         */
        router.patch("/:id")
                .handler(DiscoveryRequestValidator::updateDiscoveryRequestValidator)
                .handler(DiscoveryHandler::updateDiscovery);

        /**
         * PATCH /api/v1/discovery/credentials/:id
         *
         * To Add or remove credentials related to Discovery_profile
         */
        router.patch("/credential/:id")
                .handler(DiscoveryRequestValidator::updateDiscoveryCredentialsRequestValidator)
                .handler(DiscoveryHandler::updateDiscoveryCredentials);

        /**
         * DELETE /api/v1/discovery/:id
         */
        router.delete("/:id")
                .handler(DiscoveryRequestValidator::deleteDiscoveryRequestValidator)
                .handler(DiscoveryHandler::deleteDiscovery);

        return router;
    }
}

class CredentialRouter
{
    public static Router router()
    {
        var router = Router.router(App.vertx);

        /**
         * GET  /api/v1/credential
         */
        router.get("/")
                .handler(CredentialHandler::getAllCredentials);

        /**
         * GET  /api/v1/credential/:id
         */
        router.get("/:id")
                .handler(CredentialRequestValidator::getCredentialByIdRequestValidator)
                .handler(CredentialHandler::getCredentialById);

        /**
         * POST /api/v1/credential
         */
        router.post("/")
                .handler(CredentialRequestValidator::createCredentialRequestValidator)
                .handler(CredentialHandler::createCredential);

        /**
         * PATCH /api/v1/credential/:id
         *
         * To Update Credential_profile's Name & Username, password
         */
        router.patch("/:id")
                .handler(CredentialRequestValidator::updateCredentialByIdRequestValidator)
                .handler(CredentialHandler::updateCredential);

        /**
         * DELETE /api/v1/credential/:id
         */
        router.delete("/:id")
                .handler(CredentialRequestValidator::deleteCredentialByIdRequestValidator)
                .handler(CredentialHandler::deleteCredential);

        return router;
    }
}

class PollingRouter
{
    public static Router router()
    {
        var router = Router.router(App.vertx);

        /**
         * GET /api/v1/credential
         */
        router.get("/")
                .handler(MetricResultHandler::getAllPolledData);
        return router;
    }
}