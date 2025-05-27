package org.nms.api;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.nms.validators.Validators;
import org.nms.constants.Fields;
import org.nms.constants.Queries;
import org.nms.utils.DbUtils;

import static org.nms.App.VERTX;
import static org.nms.api.HttpServer.jwtAuthHandler;
import static org.nms.constants.Fields.ENDPOINTS.USER_ENDPOINT;
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
                .handler(this::list);

        userRouter.get("/:id")
                .handler(jwtAuthHandler)
                .handler(HttpServer::authenticate)
                .handler(this::get);

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
                .handler(this::delete);

        router.route(USER_ENDPOINT).subRouter(userRouter);

    }

    @Override
    public void list(RoutingContext ctx)
    {
        DbUtils.sendQueryExecutionRequest(Queries.User.GET_ALL).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var users = asyncResult.result();

                if (users.isEmpty())
                {
                    sendFailure(ctx, 404, "No users found");

                    return;
                }
                sendSuccess(ctx, 200, "Users found", users);
            }
            else
            {
                sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
            }
        });
    }

    @Override
    public void get(RoutingContext ctx)
    {
        var id = Validators.validateID(ctx);

        if(id == -1) { return; }

        DbUtils.sendQueryExecutionRequest(Queries.User.GET_BY_ID, new JsonArray().add(id)).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var user = asyncResult.result();

                if (user.isEmpty())
                {
                    sendFailure(ctx, 404, "User not found");
                    return;
                }

                sendSuccess(ctx, 200, "User found", user);
            }
            else
            {
                sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
            }
        });
    }

    @Override
    public void insert(RoutingContext ctx)
    {
        if(Validators.validateBody(ctx)) { return; }

        if (
                Validators.validateInputFields(ctx, new String[]{
                        Fields.User.NAME,
                        Fields.User.PASSWORD}, true)
        ) { return; }

        DbUtils.sendQueryExecutionRequest(Queries.User.GET_BY_NAME, new JsonArray().add(ctx.body().asJsonObject().getString("name")))

                .compose(user ->
                {
                    if (user != null && !user.isEmpty())
                    {
                        sendFailure(ctx, 409, "User already exists");

                        return Future.failedFuture(new Exception("User Already Exist"));
                    }

                    return Future.succeededFuture();
                })

                .compose(useExist ->

                    DbUtils.sendQueryExecutionRequest(Queries.User.INSERT, new JsonArray()
                            .add(ctx.body().asJsonObject().getString("name"))
                            .add(ctx.body().asJsonObject().getString("password"))
                    ))

                .onComplete(userInsertion ->
                 {
                    if (userInsertion.succeeded())
                    {
                        var user = userInsertion.result();

                        if (user.isEmpty())
                        {
                            sendFailure(ctx, 400, "Cannot register user");

                            return;
                        }

                        var jwtToken = HttpServer.jwtAuth
                                .generateToken(new JsonObject().put("id", user.getJsonObject(0).getInteger("id")));

                        user.getJsonObject(0).put("jwt", jwtToken);

                        sendSuccess(ctx, 201, "User registered", user);
                    }
                    else
                    {
                        sendFailure(ctx, 500, "Something Went Wrong", userInsertion.cause().getMessage());
                    }
                 });
    }

    public void login(RoutingContext ctx)
    {

        if(Validators.validateBody(ctx)) { return; }

        if (
                Validators.validateInputFields(ctx, new String[]{
                        Fields.User.NAME,
                        Fields.User.PASSWORD}, true)
        ) { return; }

        var username = ctx.body().asJsonObject().getString("name");

        DbUtils.sendQueryExecutionRequest(Queries.User.GET_BY_NAME, new JsonArray().add(username)).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var user = asyncResult.result();

                if (user == null || user.isEmpty())
                {
                    sendFailure(ctx, 404, "User not found");

                    return;
                }

                var password = ctx.body().asJsonObject().getString("password");

                if (!user.getJsonObject(0).getString("password").equals(password))
                {
                    sendFailure(ctx, 401, "Invalid password");

                    return;
                }

                var jwtToken = HttpServer.jwtAuth.generateToken(new JsonObject()
                        .put("id", user.getJsonObject(0).getInteger("id")));

                user.getJsonObject(0).put("jwt", jwtToken);

                sendSuccess(ctx, 200, "User logged in", user);
            }
            else
            {
                sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
            }
        });
    }

    @Override
    public void update(RoutingContext ctx)
    {
        var id = Validators.validateID(ctx);

        if(id == -1) { return; }

        if(Validators.validateBody(ctx)) { return; }

        if (
                Validators.validateInputFields(ctx, new String[]{
                        Fields.User.NAME,
                        Fields.User.PASSWORD}, false)
        ) { return; }

        var username = ctx.body().asJsonObject().getString("name");

        var password = ctx.body().asJsonObject().getString("password");

        DbUtils.sendQueryExecutionRequest(Queries.User.UPDATE, new JsonArray()
                .add(id)
                .add(username)
                .add(password)
        ).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var updatedUser = asyncResult.result();

                sendSuccess(ctx, 200, "User updated successfully", updatedUser);
            }
            else
            {
                sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
            }
        });
    }

    @Override
    public void delete(RoutingContext ctx)
    {
        var id = Validators.validateID(ctx);

        if(id == -1) { return; }

        DbUtils.sendQueryExecutionRequest(Queries.User.GET_BY_ID, new JsonArray().add(id)).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var user = asyncResult.result();

                if (user.isEmpty())
                {
                    sendFailure(ctx, 404, "User not found");

                    return;
                }

                var loggedInUser = ctx.user().principal().getInteger("id");

                if (id != loggedInUser)
                {
                    sendFailure(ctx, 403, "You are not authorized to delete this user");

                    return;
                }

                DbUtils.sendQueryExecutionRequest(Queries.User.DELETE, new JsonArray().add(id)).onComplete(userDeletion ->
                {
                    if (userDeletion.succeeded())
                    {
                        var deletedUser = userDeletion.result();

                        sendSuccess(ctx, 200, "User deleted Successfully", deletedUser);
                    }
                    else
                    {
                        sendFailure(ctx, 500, "Error deleting user: " + userDeletion.cause().getMessage());
                    }
                });
            }
            else
            {
                sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
            }
        });
    }
}