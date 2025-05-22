package org.nms.api;

import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.nms.validators.Validators;
import org.nms.constants.Fields;
import org.nms.constants.Queries;
import org.nms.utils.ApiUtils;

import static org.nms.App.vertx;
import static org.nms.constants.Fields.ENDPOINTS.CREDENTIALS_ENDPOINT;
import static org.nms.utils.DbUtils.sendFailure;
import static org.nms.utils.DbUtils.sendSuccess;

public class Credential implements BaseHandler
{
    private static Credential instance;

    private Credential(){}

    public static Credential getInstance()
    {
        if(instance == null)
        {
            instance = new Credential();
        }

        return instance;
    }

    @Override
    public void init(Router router)
    {
        var credentialRouter = Router.router(vertx);

        credentialRouter.get("/")
                .handler(this::list);

        credentialRouter.get("/:id")
                .handler(this::get);

        credentialRouter.post("/")
                .handler(this::insert);

        credentialRouter.patch("/:id")
                .handler(this::update);

        credentialRouter.delete("/:id")
                .handler(this::delete);

        router.route(CREDENTIALS_ENDPOINT).subRouter(credentialRouter);
    }

    @Override
    public void list(RoutingContext ctx)
    {
        ApiUtils.sendQueryExecutionRequest(Queries.Credential.GET_ALL).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var credentials = asyncResult.result();

                if (credentials.isEmpty())
                {
                    sendFailure(ctx, 404, "No credentials found");
                    return;
                }

                sendSuccess(ctx, 200, "Credentials found", credentials);
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

        var queryRequest = ApiUtils.sendQueryExecutionRequest(Queries.Credential.GET_BY_ID, new JsonArray().add(id));

        queryRequest.onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var credential = asyncResult.result();

                if (credential.isEmpty())
                {
                    sendFailure(ctx, 404, "Credential not found");

                    return;
                }
                sendSuccess(ctx, 200, "Credential found", credential);
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
        Validators.validateBody(ctx);

        if (
                Validators.validateInputFields(ctx, new String[]{
                                                        Fields.Credential.NAME,
                                                        Fields.Credential.USERNAME,
                                                        Fields.Credential.PASSWORD}, true)
        ) { return; }

        var name = ctx.body().asJsonObject().getString("name");

        var username = ctx.body().asJsonObject().getString("username");

        var password = ctx.body().asJsonObject().getString("password");

        ApiUtils.sendQueryExecutionRequest(Queries.Credential.INSERT, new JsonArray()
                .add(name)
                .add(username)
                .add(password)
        ).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var credential = asyncResult.result();
                if (credential.isEmpty())
                {
                    sendFailure(ctx, 400, "Cannot create credential");
                    return;
                }
                sendSuccess(ctx, 201, "Credential created", credential);
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
                        Fields.Credential.NAME,
                        Fields.Credential.USERNAME,
                        Fields.Credential.PASSWORD}, false)
        ) { return; }

        var checkRequest = ApiUtils.sendQueryExecutionRequest(Queries.Credential.GET_BY_ID, new JsonArray().add(id));

        checkRequest.onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var credential = asyncResult.result();

                if (credential.isEmpty())
                {
                    sendFailure(ctx, 404, "Credential not found");

                    return;
                }

                var name = ctx.body().asJsonObject().getString("name");

                var username = ctx.body().asJsonObject().getString("username");

                var password = ctx.body().asJsonObject().getString("password");

                ApiUtils.sendQueryExecutionRequest(Queries.Credential.UPDATE, new JsonArray()
                        .add(id)
                        .add(name)
                        .add(username)
                        .add(password)
                ).onComplete(updateResult ->
                {
                    if (updateResult.succeeded())
                    {
                        var res = updateResult.result();

                        sendSuccess(ctx, 200, "Credential updated successfully", res);
                    }
                    else
                    {
                        sendFailure(ctx, 500, updateResult.cause().getMessage());
                    }
                });
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

        ApiUtils.sendQueryExecutionRequest(Queries.Credential.GET_BY_ID, new JsonArray().add(id)).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var credential = asyncResult.result();

                if (credential.isEmpty())
                {
                    sendFailure(ctx, 404, "Credential not found");

                    return;
                }

                var deleteRequest = ApiUtils.sendQueryExecutionRequest(Queries.Credential.DELETE, new JsonArray().add(id));

                deleteRequest.onComplete(deleteResult ->
                {
                    if (asyncResult.succeeded())
                    {
                        var res = asyncResult.result();

                        sendSuccess(ctx, 200, "Credential deleted successfully", res);
                    }
                    else
                    {
                        sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
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