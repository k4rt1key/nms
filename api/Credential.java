package org.nms.api;

import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.nms.validators.Validators;
import org.nms.constants.Fields;
import org.nms.constants.Queries;
import org.nms.utils.DbUtils;

import static org.nms.App.VERTX;
import static org.nms.constants.Fields.Credential.*;
import static org.nms.constants.Fields.ENDPOINTS.CREDENTIALS_ENDPOINT;
import static org.nms.utils.ApiUtils.sendFailure;
import static org.nms.utils.ApiUtils.sendSuccess;

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
        var credentialRouter = Router.router(VERTX);

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
        DbUtils.execute(Queries.Credential.GET_ALL).onComplete(asyncResult ->
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

        var queryRequest = DbUtils.execute(Queries.Credential.GET_BY_ID, new JsonArray().add(id));

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
                                                        CREDENTIAL,
                }, true)
        ) { return; }

        var name = ctx.body().asJsonObject().getString(NAME);

        var credentialInput = ctx.body().asJsonObject().getJsonObject(CREDENTIAL);

        DbUtils.execute(Queries.Credential.INSERT, new JsonArray()
                .add(name)
                .add(credentialInput)
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
                if(asyncResult.cause().getMessage().contains("23505"))
                {
                    sendFailure(ctx, 500, "Error during creating credential, credential with that name already exist");
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

        if(id == -1) { return; }

        if(Validators.validateBody(ctx)) { return; }

        if (
                Validators.validateInputFields(ctx, new String[]{
                        NAME,
                        USERNAME,
                        PASSWORD,
                        PROTOCOL
                }, false)
        ) { return; }

        var checkRequest = DbUtils.execute(Queries.Credential.GET_BY_ID, new JsonArray().add(id));

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

                var name = ctx.body().asJsonObject().getString(NAME);

                var username = ctx.body().asJsonObject().getString(USERNAME);

                var password = ctx.body().asJsonObject().getString(PASSWORD);

                var protocol = ctx.body().asJsonObject().getString(PROTOCOL);

                DbUtils.execute(Queries.Credential.UPDATE, new JsonArray()
                        .add(id)
                        .add(name)
                        .add(username)
                        .add(password)
                        .add(protocol)
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

        DbUtils.execute(Queries.Credential.GET_BY_ID, new JsonArray().add(id)).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var credential = asyncResult.result();

                if (credential.isEmpty())
                {
                    sendFailure(ctx, 404, "Credential not found");

                    return;
                }

                var deleteRequest = DbUtils.execute(Queries.Credential.DELETE, new JsonArray().add(id));

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