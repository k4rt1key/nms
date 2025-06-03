package org.nms.api;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.nms.constants.DatabaseConstant;
import org.nms.constants.EventbusAddress;
import org.nms.validators.Validators;
import org.nms.constants.DatabaseQueries;
import org.nms.utils.DbUtils;

import static org.nms.App.VERTX;
import static org.nms.api.HttpServer.jwtAuthHandler;
import static org.nms.utils.ApiUtils.sendFailure;
import static org.nms.utils.ApiUtils.sendSuccess;

public class UserProfile implements AbstractHandler
{
    private static UserProfile instance;

    private UserProfile() {}

    public static UserProfile getInstance()
    {
        if (instance == null)
        {
            instance = new UserProfile();
        }

        return instance;
    }

    @Override
    public void init(Router router)
    {
        var ENDPOINT = "/api/v1/user/*";

        var userRouter = Router.router(VERTX);

        userRouter.get("/")
                .handler((ctx) -> this.list(ctx, DatabaseQueries.UserProfile.LIST));

        userRouter.get("/:id")
                .handler(jwtAuthHandler)
                .handler(HttpServer::authenticate)
                .handler((ctx) -> this.get(ctx, DatabaseQueries.UserProfile.GET));

        userRouter.post("/login")
                .handler(this::login);

        userRouter.post("/register")
                .handler(this::insert);

        userRouter.patch("/:id")
                .handler(jwtAuthHandler)
                .handler(HttpServer::authenticate)
                .handler(this::update);

        userRouter.delete("/:id")
                .handler(jwtAuthHandler)
                .handler(HttpServer::authenticate)
                .handler((ctx) -> this.delete(ctx, DatabaseQueries.UserProfile.DELETE));

        router.route(ENDPOINT).subRouter(userRouter);
    }

    @Override
    public void insert(RoutingContext ctx)
    {
        if (Validators.validateBody(ctx)) return;

        if (Validators.validateInputFields(ctx, new String[]{
                DatabaseConstant.UserProfileSchema.USERNAME,
                DatabaseConstant.UserProfileSchema.PASSWORD}, true)) return;

        var username = ctx.body().asJsonObject().getString(DatabaseConstant.UserProfileSchema.USERNAME);

        var password = ctx.body().asJsonObject().getString(DatabaseConstant.UserProfileSchema.PASSWORD);

        var queryRequest = DbUtils.buildRequest(
                DatabaseQueries.UserProfile.INSERT,
                new JsonArray().add(username).add(password),
                DatabaseConstant.SQL_QUERY_SINGLE_PARAM
        );

        VERTX.eventBus().<JsonArray>request(EventbusAddress.EXECUTE_QUERY, queryRequest, asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var userProfile = asyncResult.result().body();

                if (userProfile.isEmpty())
                {
                    sendFailure(ctx, 400, "Something Went Wrong");

                    return;
                }

                sendSuccess(ctx, 201, userProfile.getJsonObject(0));
            }
            else
            {
                if(asyncResult.cause().getMessage().contains("23505"))
                {
                    sendFailure(ctx, 500, "Error during creating user_profile, user_profile with that username already exist");
                }
                else
                {
                    sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
                }
            }
        });

    }

    @Override
    public void update(RoutingContext ctx)
    {
        var id = Validators.validateID(ctx);

        if (id == -1) return;

        if (Validators.validateBody(ctx)) return;

        if (Validators.validateInputFields(ctx, new String[]{
                DatabaseConstant.UserProfileSchema.USERNAME,
                DatabaseConstant.UserProfileSchema.PASSWORD}, false)) return;

        var username = ctx.body().asJsonObject().getString(DatabaseConstant.UserProfileSchema.USERNAME);

        var password = ctx.body().asJsonObject().getString(DatabaseConstant.UserProfileSchema.PASSWORD);

        var queryRequest = DbUtils.buildRequest(
                DatabaseQueries.UserProfile.UPDATE,
                new JsonArray().add(id).add(username).add(password),
                DatabaseConstant.SQL_QUERY_SINGLE_PARAM
        );

        VERTX.eventBus().<JsonArray>request(EventbusAddress.EXECUTE_QUERY, queryRequest, asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var updatedUser = asyncResult.result().body();

                if (updatedUser.isEmpty())
                {
                    sendFailure(ctx, 400, "Something Went Wrong");

                    return;
                }

                sendSuccess(ctx, 200, updatedUser.getJsonObject(0));
            }
            else
            {
                sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
            }
        });
    }

    public void login(RoutingContext ctx)
    {

        if (Validators.validateBody(ctx)) return;

        if (Validators.validateInputFields(ctx, new String[]{
                DatabaseConstant.UserProfileSchema.USERNAME,
                DatabaseConstant.UserProfileSchema.PASSWORD}, true)) return;

        var username = ctx.body().asJsonObject().getString(DatabaseConstant.UserProfileSchema.USERNAME);

        var password = ctx.body().asJsonObject().getString(DatabaseConstant.UserProfileSchema.PASSWORD);

        var queryRequest = DbUtils.buildRequest(
                DatabaseQueries.UserProfile.GET_BY_USERNAME,
                new JsonArray().add(username),
                DatabaseConstant.SQL_QUERY_SINGLE_PARAM
        );

        VERTX.eventBus().<JsonArray>request(EventbusAddress.EXECUTE_QUERY, queryRequest, asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var userProfile = asyncResult.result().body();

                if (userProfile.isEmpty())
                {
                    sendFailure(ctx, 404, "User not found");

                    return;
                }

                if (!userProfile.getJsonObject(0).getString(DatabaseConstant.UserProfileSchema.PASSWORD).equals(password))
                {
                    sendFailure(ctx, 401, "Invalid password");

                    return;
                }

                var jwtToken = HttpServer.jwtAuth.generateToken(
                        new JsonObject().put("id", userProfile.getJsonObject(0).getInteger("id"))
                );

                userProfile.getJsonObject(0).put("jwt", jwtToken);

                sendSuccess(ctx, 200, userProfile.getJsonObject(0));

            }
            else
            {
                sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
            }
        });
    }

    @Override
    public void updateCache(JsonArray data)
    {
        // NA
    }

    @Override
    public void insertCache(JsonArray data)
    {
        // NA
    }

    @Override
    public void deleteCache(JsonArray data)
    {
        // NA
    }
}