package org.nms.HttpServer.Routers;

import io.vertx.ext.web.Router;
import org.nms.App;
import org.nms.HttpServer.Controllers.DiscoveryController;
import org.nms.HttpServer.Middlewares.Validators.DiscoveryRequestValidator;

/**
 * Router class that handles routing for discovery-related endpoints.
 * Maps HTTP methods to appropriate controller methods and applies validation middleware.
 */
public class DiscoveryRouter
{
    /**
     * Creates and configures the router for discovery endpoints
     *
     * @return The configured router
     */
    public static Router router()
    {
        var router = Router.router(App.vertx);

        /**
         * GET  /api/v1/discoveries/results/:id
         */

        router.get("/results/:id")
                .handler(DiscoveryRequestValidator::getDiscoveryByIdRequestValidator)
                .handler(DiscoveryController::getDiscoveryResultsById);

        /**
         * GET  /api/v1/discoveries/results
         */

        router.get("/results")
                .handler(DiscoveryController::getDiscoveryResults);

        /*
         * GET  /api/v1/discoveries
         */
        router.get("/")
                .handler(DiscoveryController::getAllDiscoveries);

        /*
         * GET  /api/v1/discoveries/:id
         */
        router.get("/:id")
                .handler(DiscoveryRequestValidator::getDiscoveryByIdRequestValidator)
                .handler(DiscoveryController::getDiscoveryById);


        /*
         * POST /api/v1/discoveries
         *
         * To create Discovery_profile
         */
        router.post("/")
                .handler(DiscoveryRequestValidator::createDiscoveryRequestValidator)
                .handler(DiscoveryController::createDiscovery);


        /*
         * POST /api/v1/discoveries/run/:id
         *
         * To run Discovery
         */
        router.post("/run/:id")
                .handler(DiscoveryRequestValidator::runDiscoveryRequestValidator)
                .handler(DiscoveryController::runDiscovery);

        /*
         * PATCH /api/v1/discoveries/:id
         */
        router.patch("/:id")
                .handler(DiscoveryRequestValidator::updateDiscoveryRequestValidator)
                .handler(DiscoveryController::updateDiscovery);

        /*
         * PATCH /api/v1/discovert/credentials/:id
         *
         * To Add or remove credentials related to Discovery_profile
         */
        router.patch("/credentials/:id")
                .handler(DiscoveryRequestValidator::updateDiscoveryCredentialsRequestValidator)
                .handler(DiscoveryController::updateDiscoveryCredentials);

        /*
         * DELETE /api/v1/discoveries/:id
         */
        router.delete("/:id")
                .handler(DiscoveryRequestValidator::deleteDiscoveryRequestValidator)
                .handler(DiscoveryController::deleteDiscovery);

        return router;
    }
}