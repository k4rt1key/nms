package org.nms.api;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Tuple;
import static org.nms.App.logger;

import org.nms.validators.Validators;
import org.nms.constants.Fields;
import org.nms.constants.Queries;
import org.nms.utils.DbUtils;

import java.util.ArrayList;

import static org.nms.App.vertx;
import static org.nms.constants.Fields.ENDPOINTS.DISCOVERY_ENDPOINT;
import static org.nms.utils.ApiUtils.sendFailure;
import static org.nms.utils.ApiUtils.sendSuccess;
import static org.nms.validators.Validators.validateIpWithIpType;
import static org.nms.constants.Fields.Discovery.ADD_CREDENTIALS;
import static org.nms.constants.Fields.Discovery.REMOVE_CREDENTIALS;
import static org.nms.constants.Fields.PluginPollingRequest.CREDENTIALS;

public class Discovery implements AbstractHandler
{
    private static Discovery instance;

    private Discovery(){}

    public static Discovery getInstance()
    {
        if(instance == null)
        {
            instance = new Discovery();
        }

        return instance;
    }

    @Override
    public void init(Router router)
    {
        var discoveryRouter = Router.router(vertx);

        discoveryRouter.get("/results/:id")
                .handler(this::getDiscoveryResultsById);

        discoveryRouter.get("/results")
                .handler(this::getDiscoveryResults);

        discoveryRouter.get("/")
                .handler(this::list);

        discoveryRouter.get("/:id")
                .handler(this::get);

        discoveryRouter.post("/")
                .handler(this::insert);

        discoveryRouter.post("/run/:id")
                .handler(this::run);

        discoveryRouter.patch("/:id")
                .handler(this::update);

        discoveryRouter.patch("/credential/:id")
                .handler(this::updateDiscoveryCredentials);

        discoveryRouter.delete("/:id")
                .handler(this::delete);

        router.route(DISCOVERY_ENDPOINT).subRouter(discoveryRouter);
    }

    @Override
    public void list(RoutingContext ctx)
    {
        DbUtils.sendQueryExecutionRequest(Queries.Discovery.GET_ALL).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var discoveries = asyncResult.result();

                if (discoveries.isEmpty())
                {
                    sendFailure(ctx, 404, "No discoveries found");

                    return;
                }

                sendSuccess(ctx, 200, "Discoveries found", discoveries);
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

        DbUtils.sendQueryExecutionRequest(Queries.Discovery.GET_BY_ID, new JsonArray().add(id)).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var discovery = asyncResult.result();

                if (discovery.isEmpty())
                {
                    sendFailure(ctx, 404, "Discovery not found");

                    return;
                }

                sendSuccess(ctx, 200, "Discovery found", discovery);
            }
            else
            {
                sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
            }
        });
    }

    public void getDiscoveryResultsById(RoutingContext ctx)
    {
        var id = Validators.validateID(ctx);

        if(id == -1) { return; }

        DbUtils.sendQueryExecutionRequest(Queries.Discovery.GET_WITH_RESULTS_BY_ID, new JsonArray().add(id)).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var discovery = asyncResult.result();

                if (discovery.isEmpty())
                {
                    sendFailure(ctx, 404, "Discovery not found");

                    return;
                }

                sendSuccess(ctx, 200, "Discovery found", discovery);
            }
            else
            {
                sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
            }
        });
    }

    public void getDiscoveryResults(RoutingContext ctx)
    {
        DbUtils.sendQueryExecutionRequest(Queries.Discovery.GET_ALL_WITH_RESULTS).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var discoveries = asyncResult.result();

                if (discoveries.isEmpty())
                {
                    sendFailure(ctx, 404, "No discoveries found");

                    return;
                }

                sendSuccess(ctx, 200, "Discoveries found", discoveries);
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
        if(validateCreateDiscovery(ctx)) { return; }

        var name = ctx.body().asJsonObject().getString(Fields.Discovery.NAME);

        var ip = ctx.body().asJsonObject().getString(Fields.Discovery.IP);

        var ipType = ctx.body().asJsonObject().getString(Fields.Discovery.IP_TYPE);

        var credentials = ctx.body().asJsonObject().getJsonArray(Fields.Discovery.CREDENTIAL_JSON);

        var port = ctx.body().asJsonObject().getInteger(Fields.Discovery.PORT);

        DbUtils.sendQueryExecutionRequest(Queries.Discovery.INSERT, new JsonArray().add(name).add(ip).add(ipType).add(port)).onComplete(asyncResult ->
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

                var credentialRequest = DbUtils.sendQueryExecutionRequest(Queries.Discovery.INSERT_CREDENTIAL, credentialsToAdd);

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

                        DbUtils.sendQueryExecutionRequest(Queries.Discovery.GET_BY_ID, new JsonArray().add(discoveryId)).onComplete(discoveryResult ->
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
                        DbUtils.sendQueryExecutionRequest(Queries.Discovery.DELETE, new JsonArray().add(discoveryId)).onComplete(rollbackResult ->
                        {
                            if (rollbackResult.failed())
                            {
                                logger.error("Failed to rollback discovery creation: " + rollbackResult.cause().getMessage());
                            }
                        });

                        sendFailure(ctx, 500, "Something Went Wrong", credentialInsertion.cause().getMessage());
                    }
                });
            }
            else
            {
                sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
            }
        });
    }

    public void run(RoutingContext ctx)
    {
        try
        {
            var id = Validators.validateID(ctx);

            if(id == -1) { return; }

            DbUtils.sendQueryExecutionRequest(Queries.Discovery.GET_BY_ID, new JsonArray().add(id)).onComplete(asyncResult ->
            {
                if (asyncResult.succeeded())
                {
                    var discovery = asyncResult.result();

                    if (discovery.isEmpty())
                    {
                        sendFailure(ctx, 404, "Discovery not found");

                        return;
                    }

                    vertx.eventBus().send(
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

        DbUtils.sendQueryExecutionRequest(Queries.Discovery.GET_BY_ID, new JsonArray().add(id)).onComplete(asyncResult ->
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

                DbUtils.sendQueryExecutionRequest(Queries.Discovery.UPDATE, new JsonArray().add(id).add(name).add(ip).add(ipType).add(port)).onComplete(updateResult ->
                {
                    if (updateResult.succeeded())
                    {
                        var updatedDiscovery = updateResult.result();

                        if (updatedDiscovery.isEmpty())
                        {
                            sendFailure(ctx, 400, "Failed to update discovery");

                            return;
                        }

                        DbUtils.sendQueryExecutionRequest(Queries.Discovery.GET_BY_ID, new JsonArray().add(id)).onComplete(discoveryResult ->
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

    public void updateDiscoveryCredentials(RoutingContext ctx)
    {
        var id = Integer.parseInt(ctx.request().getParam("id"));

        if( validateUpdateDiscoveryCredential(ctx)) { return; }

        DbUtils.sendQueryExecutionRequest(Queries.Discovery.GET_BY_ID, new JsonArray().add(id)).onComplete(discoveryResult ->
        {
            if (discoveryResult.succeeded())
            {
                var discovery = discoveryResult.result();

                if (discovery.isEmpty())
                {
                    sendFailure(ctx, 404, "Discovery not found");

                    return;
                }

                var addCredentials = ctx.body().asJsonObject().getJsonArray(Fields.Discovery.ADD_CREDENTIALS);

                var removeCredentials = ctx.body().asJsonObject().getJsonArray(Fields.Discovery.REMOVE_CREDENTIALS);

                if (addCredentials != null && !addCredentials.isEmpty())
                {
                    var credentialsToAdd = new ArrayList<Tuple>();

                    for (var i = 0; i < addCredentials.size(); i++)
                    {
                        var credentialId = Integer.parseInt(addCredentials.getString(i));

                        credentialsToAdd.add(Tuple.of(id, credentialId));
                    }

                    DbUtils.sendQueryExecutionRequest(Queries.Discovery.INSERT_CREDENTIAL, credentialsToAdd).onComplete(discoveryInsertion ->
                    {
                        if (discoveryInsertion.succeeded())
                        {
                            processRemoveCredentials(ctx, id, removeCredentials);
                        }
                        else
                        {
                            sendFailure(ctx, 500, "Something Went Wrong", discoveryInsertion.cause().getMessage());
                        }
                    });
                }
                else
                {
                    processRemoveCredentials(ctx, id, removeCredentials);
                }
            }
            else
            {
                sendFailure(ctx, 500, "Something Went Wrong", discoveryResult.cause().getMessage());
            }
        });
    }

    @Override
    public void delete(RoutingContext ctx)
    {
        var id = Validators.validateID(ctx);

        if(id == -1) { return; }

        DbUtils.sendQueryExecutionRequest(Queries.Discovery.DELETE, new JsonArray().add(id)).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var discovery = asyncResult.result();

                if (discovery.isEmpty())
                {
                    sendFailure(ctx, 404, "Discovery not found");

                    return;
                }

                sendSuccess(ctx, 200, "Discovery deleted successfully", discovery);
            }
            else
            {
                sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
            }
        });
    }

    // Helper
    private void processRemoveCredentials(RoutingContext ctx, int discoveryId, JsonArray removeCredentials)
    {
        if (removeCredentials != null && !removeCredentials.isEmpty())
        {
            var credentialsToRemove = new ArrayList<Tuple>();

            for (var i = 0; i < removeCredentials.size(); i++)
            {
                var credentialId = Integer.parseInt(removeCredentials.getString(i));

                credentialsToRemove.add(Tuple.of(discoveryId, credentialId));
            }

            DbUtils.sendQueryExecutionRequest(Queries.Discovery.DELETE_CREDENTIAL, credentialsToRemove).onComplete(asyncResult ->
            {
                if (asyncResult.succeeded())
                {
                    returnUpdatedDiscovery(ctx, discoveryId);
                }
                else
                {
                    sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
                }
            });
        }
        else
        {
            returnUpdatedDiscovery(ctx, discoveryId);
        }
    }

    // Helper
    private void returnUpdatedDiscovery(RoutingContext ctx, int discoveryId)
    {
        DbUtils.sendQueryExecutionRequest(Queries.Discovery.GET_BY_ID, new JsonArray().add(discoveryId)).onComplete(asyncResult ->
        {
            if (asyncResult.succeeded())
            {
                var updatedDiscovery = asyncResult.result();

                sendSuccess(ctx, 200, "Discovery credentials updated successfully", updatedDiscovery);
            }
            else
            {
                sendFailure(ctx, 500, "Something Went Wrong", asyncResult.cause().getMessage());
            }
        });
    }

    // Validator
    private boolean validateCreateDiscovery(RoutingContext ctx)
    {

        if(Validators.validateBody(ctx)) { return true; }

        if(Validators.validateInputFields(ctx, new String[]{Fields.Discovery.IP, Fields.Discovery.IP_TYPE, Fields.Discovery.NAME, Fields.Discovery.PORT}, true)) { return true; }

        var body = ctx.body().asJsonObject();

        var credentials = body.getJsonArray(CREDENTIALS);

        if (credentials == null || credentials.isEmpty())
        {
            sendFailure(ctx, 400, "Missing or empty 'credentials' field");

            return true;
        }

        for (var credential : credentials)
        {
            try
            {
                Integer.parseInt(credential.toString());
            }
            catch (NumberFormatException exception)
            {
                sendFailure(ctx, 400, "Invalid credential ID: " + credential);

                return true;
            }
        }

        var port = body.getInteger(Fields.Discovery.PORT);

        if (port < 1 || port > 65535)
        {
            sendFailure(ctx, 400, "Port must be between 1 and 65535");
            return true;
        }

        var ipType = body.getString(Fields.Discovery.IP_TYPE);

        if (!ipType.equals("SINGLE") && !ipType.equals("RANGE") && !ipType.equals("CIDR"))
        {
            sendFailure(ctx, 400, "Invalid IP type. Only 'SINGLE', 'RANGE' and 'CIDR' are supported");

            return true;
        }

        var ip = body.getString(Fields.Discovery.IP);

        if (validateIpWithIpType(ip, ipType))
        {
            sendFailure(ctx, 400, "Invalid IP address or mismatch Ip and IpType: " + ip + ", " + ipType);

            return true;
        }

        return false;
    }

    // Validator
    private boolean validateUpdateDiscovery(RoutingContext ctx)
    {
        if(Validators.validateBody(ctx)) { return true; }

        if(Validators.validateInputFields(ctx, new String[]{Fields.Discovery.IP, Fields.Discovery.IP_TYPE, Fields.Discovery.NAME, Fields.Discovery.PORT}, false)) { return true; }

        if(! Validators.validatePort(ctx)) { return true; }

        var ipType = ctx.body().asJsonObject().getString(Fields.Discovery.IP_TYPE);

        if (ipType != null && !ipType.equals("SINGLE") && !ipType.equals("RANGE") && !ipType.equals("CIDR"))
        {
            sendFailure(ctx, 400, "Invalid IP type. Only 'SINGLE', 'RANGE' and 'CIDR' are supported");
        }

        var ip = ctx.body().asJsonObject().getString(Fields.Discovery.IP);

        if (ip != null && ipType != null && validateIpWithIpType(ip, ipType))
        {
            sendFailure(ctx, 400, "Invalid IP address or mismatch Ip and IpType: " + ip + ", " + ipType);

            return true;
        }

        return false;
    }

    // Validator
    private boolean validateUpdateDiscoveryCredential(RoutingContext ctx)
    {
        if(Validators.validateBody(ctx)) { return true; }

        var body = ctx.body().asJsonObject();

        var add_credentials = body.getJsonArray(ADD_CREDENTIALS);

        var remove_credentials = body.getJsonArray(REMOVE_CREDENTIALS);

        if (
                (add_credentials == null || add_credentials.isEmpty()) &&
                        (remove_credentials == null || remove_credentials.isEmpty())
        )
        {
            sendFailure(ctx, 400, "Missing or empty ' " + ADD_CREDENTIALS  + " ' and ' " + REMOVE_CREDENTIALS + " ' fields");

            return true;
        }

        if (add_credentials != null && !add_credentials.isEmpty())
        {
            for (var credential : add_credentials)
            {
                try
                {
                    Integer.parseInt(credential.toString());
                }
                catch (NumberFormatException exception)
                {
                    sendFailure(ctx, 400, "Invalid credential ID in " + ADD_CREDENTIALS + " : " + credential);

                    return true;
                }
            }
        }

        if (remove_credentials != null && !remove_credentials.isEmpty())
        {
            for (var credential : remove_credentials)
            {
                try
                {
                    Integer.parseInt(credential.toString());
                }
                catch (NumberFormatException exception)
                {
                    sendFailure(ctx, 400, "Invalid credential ID in " + REMOVE_CREDENTIALS + " : " + credential);

                    return true;
                }
            }
        }

        return false;
    }
}