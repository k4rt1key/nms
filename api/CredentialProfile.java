package org.nms.api;

import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.nms.constants.DatabaseConstant;
import org.nms.constants.EventbusAddress;
import org.nms.validators.Validators;
import org.nms.constants.DatabaseQueries;
import org.nms.utils.DbUtils;

import static org.nms.App.VERTX;
import static org.nms.utils.ApiUtils.sendFailure;
import static org.nms.utils.ApiUtils.sendSuccess;
import static org.nms.constants.DatabaseConstant.CredentialProfileSchema.*;

public class CredentialProfile implements AbstractHandler
{
    private static CredentialProfile instance;

    private CredentialProfile(){}

    public static CredentialProfile getInstance()
    {
        if(instance == null)
        {
            instance = new CredentialProfile();
        }

        return instance;
    }

    @Override
    public void init(Router router)
    {
        var ENDPOINT = "/api/v1/credential/*";

        var credentialRouter = Router.router(VERTX);

        credentialRouter.get("/")
                .handler((ctx) -> this.list(ctx, DatabaseQueries.UserProfile.LIST));

        credentialRouter.get("/:id")
                .handler((ctx) -> this.get(ctx, DatabaseQueries.UserProfile.GET));

        credentialRouter.post("/")
                .handler(this::insert);

        credentialRouter.patch("/:id")
                .handler(this::update);

        credentialRouter.delete("/:id")
                .handler((ctx) -> this.delete(ctx, DatabaseQueries.UserProfile.DELETE));

        router.route(ENDPOINT).subRouter(credentialRouter);
    }

    @Override
    public void insert(RoutingContext ctx)
    {
        Validators.validateBody(ctx);

        if (
                Validators.validateInputFields(ctx, new String[]{
                                                        NAME,
                                                        CREDENTIAL,
                                                        DEVICE_TYPE
                }, true)
        ) { return; }

        var name = ctx.body().asJsonObject().getString(NAME);

        var credential = ctx.body().asJsonObject().getJsonObject(CREDENTIAL);

        var queryRequest = DbUtils.buildRequest(
                DatabaseQueries.CredentialProfile.INSERT,
                new JsonArray().add(name).add(credential),
                DatabaseConstant.SQL_QUERY_SINGLE_PARAM
        );

        VERTX.eventBus().<JsonArray>request(EventbusAddress.EXECUTE_QUERY, queryRequest, asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var credentialProfile = asyncResult.result().body();

                if (credentialProfile.isEmpty())
                {
                    sendFailure(ctx, 400, "Something Went Wrong");

                    return;
                }

                sendSuccess(ctx, 201, credentialProfile.getJsonObject(0));
            }
            else
            {
                if(asyncResult.cause().getMessage().contains("23505"))
                {
                    sendFailure(ctx, 500, "Error during creating credential_profile, credential_profile with that name already exist");
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
                        CREDENTIAL,
                        DEVICE_TYPE
                }, false)
        ) return;

        var name = ctx.body().asJsonObject().getString(NAME);

        var credential = ctx.body().asJsonObject().getString(CREDENTIAL);

        var deviceType = ctx.body().asJsonObject().getString(DEVICE_TYPE);

        var queryRequest = DbUtils.buildRequest(
                DatabaseQueries.CredentialProfile.UPDATE,
                new JsonArray().add(id).add(name).add(credential).add(deviceType),
                DatabaseConstant.SQL_QUERY_SINGLE_PARAM
        );

        VERTX.eventBus().<JsonArray>request(EventbusAddress.EXECUTE_QUERY, queryRequest, asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                sendSuccess(ctx, 200, asyncResult.result().body().getJsonObject(0));
            }
            else
            {
                sendFailure(ctx, 500, asyncResult.cause().getMessage());
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