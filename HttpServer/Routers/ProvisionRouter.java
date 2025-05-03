package org.nms.HttpServer.Routers;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.JWTAuthHandler;
import org.nms.App;
import org.nms.HttpServer.Controllers.ProvisionController;
import org.nms.HttpServer.JwtConfig;
import org.nms.HttpServer.Middlewares.Validators.ProvisionRequestValidator;

/**
 * Router class that handles routing for provision-related endpoints.
 * Maps HTTP methods to appropriate controller methods and applies validation middleware.
 */
public class ProvisionRouter
{
    /**
     * Creates and configures the router for provision endpoints
     *
     * @return The configured router
     */
    public static Router router()
    {
        var router = Router.router(App.vertx);

        /*
         * GET  /api/v1/provisions
         */
        router.get("/")
                .handler(JWTAuthHandler.create(JwtConfig.jwtAuth))
                .handler(ProvisionController::getAllProvisions);

        /*
         * GET  /api/v1/provisions/:id
         */
        router.get("/:id")
                .handler(ProvisionRequestValidator::getProvisionByIdRequestValidator)
                .handler(ProvisionController::getProvisionById);

        /*
         * POST /api/v1/provisions
         */
        router.post("/")
                .handler(ProvisionRequestValidator::createProvisionRequestValidator)
                .handler(ProvisionController::createProvision);


        /*
         * PATCH /api/v1/provisions/:id
         *
         * To Update Provisioned device's metric data such as
         * - Add metric to poll
         * - Remove metric to poll
         * - Change polling interval of metric
         */
        router.patch("/:id")
                .handler(ProvisionRequestValidator::updateProvisionRequestValidator)
                .handler(ProvisionController::updateMetric);

        /*
         * DELETE /api/v1/provisions/:id
         */
        router.delete("/:id")
                .handler(ProvisionRequestValidator::deleteProvisionRequestValidator)
                .handler(ProvisionController::deleteProvision);


        return router;
    }
}