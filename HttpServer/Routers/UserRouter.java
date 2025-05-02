package org.nms.HttpServer.Routers;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.JWTAuthHandler;
import org.nms.App;
import org.nms.HttpServer.Controllers.UserController;
import org.nms.HttpServer.JwtConfig;
import org.nms.HttpServer.Middlewares.AuthMiddleware;
import org.nms.HttpServer.Middlewares.Validators.UserRequestValidator;

/**
 * Router class that handles routing for user-related endpoints.
 * Maps HTTP methods to appropriate controller methods and applies validation middleware.
 */
public class UserRouter
{
    /**
     * Creates and configures the router for user endpoints
     *
     * @return The configured router
     */
    public static Router router()
    {
        var router = Router.router(App.vertx);

        /**
         * GET /api/v1/user/
         */
        router.get("/")
                .handler(UserController::getUsers);


        /*
         * GET  /api/v1/user/:id
         */
        router.get("/:id")
                .handler(JWTAuthHandler.create(JwtConfig.jwtAuth))
                .handler(AuthMiddleware::authenticate)
                .handler(UserRequestValidator::getUserByIdRequestValidator)
                .handler(UserController::getUserById);

        /*
         * POST /api/v1/users/login
         */
        router.post("/login")
                .handler(UserRequestValidator::loginRequestValidator)
                .handler(UserController::login);

        /*
         * POST /api/v1/users/register
         */
        router.post("/register")
                .handler(UserRequestValidator::registerRequestValidator)
                .handler(UserController::register);

        /*
         * PATCH /api/v1/users/:id
         */
        router.patch("/:id")
                .handler(JWTAuthHandler.create(JwtConfig.jwtAuth))
                .handler(AuthMiddleware::authenticate)
                .handler(UserRequestValidator::updateUserRequestValidator)
                .handler(UserController::updateUser);

        /*
         * DELETE /api/v1/users/:id
         */
        router.delete("/:id")
                .handler(JWTAuthHandler.create(JwtConfig.jwtAuth))
                .handler(AuthMiddleware::authenticate)
                .handler(UserRequestValidator::deleteUserRequestValidator)
                .handler(UserController::deleteUser);

        return router;
    }
}