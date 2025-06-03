package org.nms.api;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Tuple;
import static org.nms.App.LOGGER;

import org.nms.App;
import org.nms.constants.DatabaseConstant;
import org.nms.constants.EventbusAddress;
import org.nms.utils.ApiUtils;
import org.nms.validators.Validators;
import org.nms.constants.DatabaseQueries;
import org.nms.utils.DbUtils;

import java.util.ArrayList;

import static org.nms.App.VERTX;
import static org.nms.constants.Fields.ENDPOINTS.DISCOVERY_ENDPOINT;
import static org.nms.utils.ApiUtils.sendFailure;
import static org.nms.utils.ApiUtils.sendSuccess;
import static org.nms.validators.Validators.validateIpWithIpType;
import static org.nms.constants.Fields.Discovery.ADD_CREDENTIALS;
import static org.nms.constants.Fields.Discovery.REMOVE_CREDENTIALS;
import static org.nms.constants.Fields.PluginPollingRequest.CREDENTIALS;

public class DiscoveryProfile implements AbstractHandler
{
    private static DiscoveryProfile instance;

    private DiscoveryProfile(){}

    public static DiscoveryProfile getInstance()
    {
        if(instance == null)
        {
            instance = new DiscoveryProfile();
        }

        return instance;
    }

    @Override
    public void init(Router router)
    {
        var discoveryRouter = Router.router(VERTX);

        var DISCOVERY_ENDPOINT = "/api/v1/discovery/*";

        discoveryRouter.get("/")
                .handler((ctx) -> this.list(ctx, DatabaseQueries.DiscoveryProfile.LIST));

        discoveryRouter.get("/:id")
                .handler((ctx) -> this.get(ctx, DatabaseQueries.DiscoveryProfile.GET));

        discoveryRouter.post("/")
                .handler(this::insert);

        discoveryRouter.post("/run/:id")
                .handler(this::run);

        discoveryRouter.patch("/:id")
                .handler(this::update);

        discoveryRouter.delete("/:id")
                .handler((ctx) -> this.delete(ctx, DatabaseQueries.DiscoveryProfile.DELETE));

        discoveryRouter.post("/credential/")
                .handler((ctx) -> this.addCredential(ctx, DatabaseQueries.DiscoveryCredential.INSERT));

        discoveryRouter.delete("/credential/:discovery_profile/:credential_profile")
                .handler((ctx) -> this.removeCredential(ctx, DatabaseQueries.DiscoveryCredential.DELETE));

        discoveryRouter.get("/results/:id")
                .handler((ctx) -> this.get(ctx, DatabaseQueries.DiscoveryResult.GET));

        router.route(DISCOVERY_ENDPOINT).subRouter(discoveryRouter);
    }


    @Override
    public void insert(RoutingContext ctx)
    {
        if(validateCreate(ctx)) { return; }

        var name = ctx.body().asJsonObject().getString(Fields.Discovery.NAME);

        var ip = ctx.body().asJsonObject().getString(Fields.Discovery.IP);

        var ipType = ctx.body().asJsonObject().getString(Fields.Discovery.IP_TYPE);

        var credentials = ctx.body().asJsonObject().getJsonArray(Fields.Discovery.CREDENTIAL_JSON);

        var port = ctx.body().asJsonObject().getInteger(Fields.Discovery.PORT);

        DbUtils.execute(DatabaseQueries.Discovery.INSERT, new JsonArray().add(name).add(ip).add(ipType).add(port)).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var discovery = asyncResult.result();

                if (discovery.isEmpty())
                {
                    sendFailure(ctx, 400, "Failed to create discovery");

                    return;
                }

                var credentialsToAdd = new ArrayList<Tuple>();

                var discoveryId = discovery.getJsonObject(0).getInteger(Fields.Discovery.ID);

                for (var i = 0; i < credentials.size(); i++)
                {
                    var credentialId = Integer.parseInt(credentials.getString(i));

                    credentialsToAdd.add(Tuple.of(discoveryId, credentialId));
                }

                var credentialRequest = DbUtils.execute(DatabaseQueries.Discovery.INSERT_CREDENTIAL, credentialsToAdd);

                credentialRequest.onComplete(credentialInsertion ->
                {
                    if (credentialInsertion.succeeded())
                    {
                        var discoveryCredentials = credentialInsertion.result();

                        if (discoveryCredentials.isEmpty())
                        {
                            sendFailure(ctx, 400, "Failed to add credentials to discovery");
                            return;
                        }

                        DbUtils.execute(DatabaseQueries.Discovery.GET_BY_ID, new JsonArray().add(discoveryId)).onComplete(discoveryResult ->
                        {
                            if (discoveryResult.succeeded())
                            {
                                var discoveryWithCredentials = discoveryResult.result();

                                sendSuccess(ctx, 201, "Discovery created successfully", discoveryWithCredentials);
                            }
                            else
                            {
                                sendFailure(ctx, 500, "Something Went Wrong", discoveryResult.cause().getMessage());
                            }
                        });
                    }
                    else
                    {
                        DbUtils.execute(DatabaseQueries.Discovery.DELETE, new JsonArray().add(discoveryId)).onComplete(rollbackResult ->
                        {
                            if (rollbackResult.failed())
                            {
                                LOGGER.error("Failed to rollback discovery creation: " + rollbackResult.cause().getMessage());
                            }
                        });

                        sendFailure(ctx, 500, "Something Went Wrong", credentialInsertion.cause().getMessage());
                    }
                });
            }
            else
            {
                if(asyncResult.cause().getMessage().contains("23505"))
                {
                    sendFailure(ctx, 500, "Error during creating discovery, discovery with that name already exist");
                }
                else
                {
                    sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
                }
            }
        });
    }

    public void run(RoutingContext ctx)
    {
        try
        {
            var id = Validators.validateID(ctx);

            if(id == -1) { return; }

            DbUtils.execute(DatabaseQueries.Discovery.GET_BY_ID, new JsonArray().add(id)).onComplete(asyncResult ->
            {
                if (asyncResult.succeeded())
                {
                    var discovery = asyncResult.result();

                    if (discovery.isEmpty())
                    {
                        sendFailure(ctx, 404, "Discovery not found");

                        return;
                    }

                    VERTX.eventBus().send(
                            Fields.EventBus.RUN_DISCOVERY_ADDRESS,
                            new JsonObject().put(Fields.Discovery.ID, id)
                    );

                    sendSuccess(ctx, 202, "Discovery request with id " + id + " accepted and is being processed", new JsonArray());
                }
                else
                {
                    sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
                }
            });
        }
        catch (Exception exception)
        {
            sendFailure(ctx, 400, "Invalid discovery ID provided");
        }
    }

    @Override
    public void update(RoutingContext ctx)
    {
        var id = Integer.parseInt(ctx.request().getParam("id"));

        if(validateUpdateDiscovery(ctx)) { return; }

        DbUtils.execute(DatabaseQueries.Discovery.GET_BY_ID, new JsonArray().add(id)).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var discovery = asyncResult.result();

                if (discovery.isEmpty())
                {
                    sendFailure(ctx, 404, "Discovery not found");

                    return;
                }

                if (discovery.getJsonObject(0).getString(Fields.Discovery.STATUS).equals(Fields.DiscoveryResult.COMPLETED_STATUS))
                {
                    sendFailure(ctx, 400, "Discovery Already Run");

                    return;
                }

                var name = ctx.body().asJsonObject().getString(Fields.Discovery.NAME);

                var ip = ctx.body().asJsonObject().getString(Fields.Discovery.IP);

                var ipType = ctx.body().asJsonObject().getString(Fields.Discovery.IP_TYPE);

                var port = ctx.body().asJsonObject().getInteger(Fields.Discovery.PORT);

                DbUtils.execute(DatabaseQueries.Discovery.UPDATE, new JsonArray().add(id).add(name).add(ip).add(ipType).add(port)).onComplete(updateResult ->
                {
                    if (updateResult.succeeded())
                    {
                        var updatedDiscovery = updateResult.result();

                        if (updatedDiscovery.isEmpty())
                        {
                            sendFailure(ctx, 400, "Failed to update discovery");

                            return;
                        }

                        DbUtils.execute(DatabaseQueries.Discovery.GET_BY_ID, new JsonArray().add(id)).onComplete(discoveryResult ->
                        {
                            if (discoveryResult.succeeded())
                            {
                                var discoveryWithCredentials = discoveryResult.result();

                                sendSuccess(ctx, 200, "Discovery updated successfully", discoveryWithCredentials);
                            }
                            else
                            {
                                sendFailure(ctx, 500, "Something Went Wrong", discoveryResult.cause().getMessage());
                            }
                        });
                    }
                    else
                    {
                        sendFailure(ctx, 500, "Something Went Wrong", updateResult.cause().getMessage());
                    }
                });
            }
            else
            {
                sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
            }
        });
    }

    public void removeCredential(RoutingContext ctx, String query)
    {
        var discoveryProfile = ctx.body().asJsonObject().getInteger(DatabaseConstant.DiscoveryCredentialSchema.DISCOVERY_PROFILE);

        var credentialProfile = ctx.body().asJsonObject().getInteger(DatabaseConstant.DiscoveryCredentialSchema.CREDENTIAL_PROFILE);

        var queryRequest = DbUtils.buildRequest(
                query,
                new JsonArray().add(discoveryProfile).add(credentialProfile),
                DatabaseConstant.SQL_QUERY_SINGLE_PARAM
        );

        App.VERTX.eventBus().<JsonArray>request(EventbusAddress.EXECUTE_QUERY, queryRequest, dbResponse ->
        {
            if(dbResponse.succeeded())
            {
                if(dbResponse.result().body() == null || dbResponse.result().body().isEmpty())
                {
                    ApiUtils.sendFailure(ctx, 404, "No Data Found");
                }
                else
                {
                    ApiUtils.sendSuccess(ctx, 200, dbResponse.result().body());

                    deleteCache(dbResponse.result().body());
                }
            }
            else
            {
                ApiUtils.sendFailure(ctx, 500, "Something Went Wrong", dbResponse.cause().getMessage());
            }
        });
    }

    public void addCredential(RoutingContext ctx, String query)
    {
        var discoveryProfile = ctx.body().asJsonObject().getInteger(DatabaseConstant.DiscoveryCredentialSchema.DISCOVERY_PROFILE);

        var credentialProfile = ctx.body().asJsonObject().getInteger(DatabaseConstant.DiscoveryCredentialSchema.CREDENTIAL_PROFILE);

        var queryRequest = DbUtils.buildRequest(
                query,
                new JsonArray().add(discoveryProfile).add(credentialProfile),
                DatabaseConstant.SQL_QUERY_SINGLE_PARAM
        );

        App.VERTX.eventBus().<JsonArray>request(EventbusAddress.EXECUTE_QUERY, queryRequest, dbResponse ->
        {
            if(dbResponse.succeeded())
            {
                if(dbResponse.result().body() == null || dbResponse.result().body().isEmpty())
                {
                    ApiUtils.sendFailure(ctx, 404, "No Data Found");
                }
                else
                {
                    ApiUtils.sendSuccess(ctx, 200, dbResponse.result().body());

                    deleteCache(dbResponse.result().body());
                }
            }
            else
            {
                ApiUtils.sendFailure(ctx, 500, "Something Went Wrong", dbResponse.cause().getMessage());
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