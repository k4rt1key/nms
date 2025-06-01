package org.nms.api;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import org.nms.constants.Database;
import org.nms.constants.Eventbus;
import org.nms.constants.Global;
import org.nms.utils.ApiUtils;
import org.nms.utils.DbUtils;
import org.nms.validators.Validators;

import static org.nms.App.VERTX;

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
        var discoveryRouter = Router.router(VERTX);

        discoveryRouter.get("/:id")
                .handler((ctx) -> this.get(ctx, Database.Table.DISCOVERY_PROFILE));

        discoveryRouter.get("/credential/:discovery_profile")
                .handler((ctx) -> this.get(ctx, Database.Table.DISCOVERY_CREDENTIAL));

        discoveryRouter.get("/")
                .handler((ctx) -> this.list(ctx, Database.Table.DISCOVERY_PROFILE));

        discoveryRouter.post("/")
                .handler((ctx) -> this.insert(ctx, Database.Table.DISCOVERY_PROFILE));

        discoveryRouter.patch("/:id")
                .handler((ctx) -> this.update(ctx, Database.Table.DISCOVERY_PROFILE));

        discoveryRouter.delete("/:id")
                .handler((ctx) -> this.delete(ctx, Database.Table.DISCOVERY_PROFILE));

        router.route(Global.DISCOVERY_ENDPOINT).subRouter(discoveryRouter);
    }

    @Override
    public void insert(RoutingContext ctx, String tableName)
    {
        if(validateCreateDiscovery(ctx)) { return; }

        var requestBody = ctx.body().asJsonObject();

        var discoveryData = new JsonObject()
                .put(Database.Discovery.IP, requestBody.getString(Database.Discovery.IP))
                .put(Database.Discovery.IP_TYPE, requestBody.getString(Database.Discovery.IP_TYPE))
                .put(Database.Discovery.NAME, requestBody.getString(Database.Discovery.NAME))
                .put(Database.Discovery.PORT, requestBody.getInteger(Database.Discovery.PORT));

        var discoveryInsertRequest = DbUtils.buildRequest(
                Database.Table.DISCOVERY_PROFILE,
                Database.Operation.INSERT,
                null,
                discoveryData,
                null
        );

        VERTX.eventBus().<JsonArray>request(Eventbus.EXECUTE_QUERY, discoveryInsertRequest, discoveryInsertResult ->
        {
            if(discoveryInsertResult.failed())
            {
                ApiUtils.sendFailure(ctx, 500, "Something went wrong","Failed to INSERT discovery, Cause: " + discoveryInsertResult.cause().getMessage());

                return;
            }

            var insertedDiscovery = discoveryInsertResult.result().body().getJsonObject(0);

            var discoveryId = insertedDiscovery.getInteger(Database.Discovery.ID);

            var credentialIds = requestBody.getJsonArray(Database.Table.CREDENTIAL_PROFILE);

            var discoveryCredentialRecords = new JsonArray();

            for (var credentialId : credentialIds)
            {
                discoveryCredentialRecords.add(new JsonObject()
                        .put(Database.DiscoveryCredential.CREDENTIAL_PROFILE, Integer.parseInt(credentialId.toString()))
                        .put(Database.DiscoveryCredential.DISCOVERY_PROFILE, discoveryId));
            }

            var credentialInsertRequest = DbUtils.buildRequest(
                    Database.Table.DISCOVERY_CREDENTIAL,
                    Database.Operation.BATCH_INSERT,
                    null,
                    null,
                    discoveryCredentialRecords
            );

            VERTX.eventBus().<JsonArray>request(Eventbus.EXECUTE_QUERY, credentialInsertRequest, credentialInsertResult ->
            {
                if(credentialInsertResult.failed())
                {
                    ApiUtils.sendFailure(ctx, 500, "Something went wrong","Failed to INSERT discovery-credential, Cause: " + discoveryInsertResult.cause().getMessage());

                    return;
                }

                var insertedCredentials = credentialInsertResult.result().body();

                insertedDiscovery.put(Database.DiscoveryCredential.CREDENTIAL_PROFILE, insertedCredentials);

                var responseArray = new JsonArray().add(insertedDiscovery);

                ApiUtils.sendSuccess(ctx, 201, "Successfully INSERT discovery", responseArray);

            });
        });
    }

    private boolean validateCreateDiscovery(RoutingContext ctx)
    {

        if(Validators.validateBody(ctx))  return true;

        if(Validators.validateInputFields(ctx, new String[]
                {
                        Database.Discovery.IP,
                        Database.Discovery.IP_TYPE,
                        Database.Discovery.NAME,
                        Database.Discovery.PORT,
                }, true)) { return true; }

        var body = ctx.body().asJsonObject();

        var credentials = body.getJsonArray(Database.DiscoveryCredential.CREDENTIAL_PROFILE);

        if (credentials == null || credentials.isEmpty())
        {
            ApiUtils.sendFailure(ctx, 400, "Missing or empty 'credentials' field");

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
                ApiUtils.sendFailure(ctx, 400, "Invalid credential ID: " + credential);

                return true;
            }
        }

        var port = body.getInteger(Database.Discovery.PORT);

        if (port < 1 || port > 65535)
        {
            ApiUtils.sendFailure(ctx, 400, "Port must be between 1 and 65535");

            return true;
        }

        var ipType = body.getString(Database.Discovery.IP_TYPE);

        if (!ipType.equals("SINGLE") && !ipType.equals("RANGE") && !ipType.equals("CIDR"))
        {
            ApiUtils.sendFailure(ctx, 400, "Invalid IP type. Only 'SINGLE', 'RANGE' and 'CIDR' are supported");

            return true;
        }

        var ip = body.getString(Database.Discovery.IP);

        if (Validators.validateIpWithIpType(ip, ipType))
        {
            ApiUtils.sendFailure(ctx, 400, "Invalid IP address or mismatch Ip and IpType: " + ip + ", " + ipType);

            return true;
        }

        return false;
    }

}