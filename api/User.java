package org.nms.api;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.nms.App;
import org.nms.constants.Database;
import org.nms.validators.Validators;
import org.nms.constants.Fields;
import org.nms.constants.Queries;
import org.nms.utils.DbUtils;

import static org.nms.App.VERTX;
import static org.nms.api.HttpServer.jwtAuthHandler;
import static org.nms.constants.Eventbus.EXECUTE_QUERY;
import static org.nms.constants.Fields.ENDPOINTS.USER_ENDPOINT;
import static org.nms.constants.Fields.User.*;
import static org.nms.utils.ApiUtils.sendFailure;
import static org.nms.utils.ApiUtils.sendSuccess;

public class User implements BaseHandler
{
    private static User instance;

    private User(){}

    public static User getInstance()
    {
        if(instance == null)
        {
            instance = new User();
        }

        return instance;
    }

    @Override
    public void init(Router router)
    {
        var userRouter = Router.router(VERTX);

        userRouter.get("/")
                .handler((ctx) -> this.list(ctx, Database.Table.USER));

        userRouter.get("/:id")
                .handler(jwtAuthHandler)
                .handler(HttpServer::authenticate)
                .handler((ctx) -> this.get(ctx, Database.Table.USER));

        userRouter.post("/login")
                .handler(this::login);

        userRouter.post("/register")
                .handler((ctx) -> this.insert(ctx, Database.Table.USER));

        userRouter.patch("/:id")
                .handler(jwtAuthHandler)
                .handler(HttpServer::authenticate)
                .handler((ctx) -> this.update(ctx, Database.Table.USER));

        userRouter.delete("/:id")
                .handler(jwtAuthHandler)
                .handler(HttpServer::authenticate)
                .handler((ctx) -> this.delete(ctx, Database.Table.USER));

        router.route(USER_ENDPOINT).subRouter(userRouter);

    }

    @Override
    public void beforeInsert(RoutingContext ctx)
    {

    }

    @Override
    public void afterInsert(RoutingContext ctx)
    {

    }

    @Override
    public void beforeUpdate(RoutingContext ctx)
    {

    }

    @Override
    public void afterUpdate(RoutingContext ctx)
    {

    }

    public void login(RoutingContext ctx)
    {

        if(Validators.validateBody(ctx)) return;

        if (
                Validators.validateInputFields(ctx, new String[]{
                        NAME,
                        PASSWORD
                }, true)
        )  return;

        var username = ctx.body().asJsonObject().getString(NAME);

        var queryRequest = DbUtils.buildRequest(
                Database.Table.USER,
                Database.Operation.GET,
                new JsonArray().add(new JsonObject().put(Fields.User.NAME, username)),
                null
        );

        VERTX.eventBus().<JsonArray>request(EXECUTE_QUERY, queryRequest, dbResponse ->
        {
            if (dbResponse.succeeded())
            {
                var user = dbResponse.result().body();

                if (user == null || user.isEmpty())
                {
                    sendFailure(ctx, 404, "User not found");

                    return;
                }

                var password = ctx.body().asJsonObject().getString(PASSWORD);

                if (!user.getJsonObject(0).getString(PASSWORD).equals(password))
                {
                    sendFailure(ctx, 401, "Invalid password");

                    return;
                }

                var jwtToken = HttpServer
                        .jwtAuth
                        .generateToken(
                                new JsonObject()
                                        .put(ID, user.getJsonObject(0).getInteger(ID))
                        );

                user.getJsonObject(0).put(JWT_KEY, jwtToken);

                sendSuccess(ctx, 200, "User logged in", user);
            }
            else
            {
                sendFailure(ctx, 500, "Something Went Wrong", dbResponse.cause().getMessage());
            }
        });
    }
}