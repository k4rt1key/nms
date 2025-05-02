package org.nms.HttpServer.Routers;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.JWTAuthHandler;
import org.nms.App;
import org.nms.HttpServer.Controllers.CredentialController;
import org.nms.HttpServer.JwtConfig;
import org.nms.HttpServer.Middlewares.Validators.CredentialRequestValidator;

/**
 * Router class that handles routing for credential-related endpoints.
 * Maps HTTP methods to appropriate controller methods and applies validation middleware.
 */
public class CredentialRouter
{
    /**
     * Creates and configures the router for credential endpoints
     *
     * @return The configured router
     */
    public static Router router()
    {
        var router = Router.router(App.vertx);

        /*
         * GET  /api/v1/credentials
         */
        router.get("/")
                .handler(JWTAuthHandler.create(JwtConfig.jwtAuth))
//                .handler(AuthorizationHandler::isUser)
                .handler(CredentialController::getAllCredentials);

        /*
         * GET  /api/v1/credentials/:id
         */
        router.get("/:id")
                .handler(CredentialRequestValidator::getCredentialByIdRequestValidator)
                .handler(CredentialController::getCredentialById);

        /*
         * POST /api/v1/credentials
         */
        router.post("/")
                .handler(CredentialRequestValidator::createCredentialRequestValidator)
                .handler(CredentialController::createCredential);

        /*
         * PATCH /api/v1/credentials/:id
         *
         * To Update Credential_profile's Name & Username, password
         */
        router.patch("/:id")
                .handler(CredentialRequestValidator::updateCredentialByIdRequestValidator)
                .handler(CredentialController::updateCredential);

        /*
         * DELETE /api/v1/credentials/:id
         */
        router.delete("/:id")
                .handler(CredentialRequestValidator::deleteCredentialByIdRequestValidator)
                .handler(CredentialController::deleteCredential);

        return router;
    }
}