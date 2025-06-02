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

    private Discovery()
    {

    }

    public static Discovery getInstance()
    {
        if (instance == null)
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
                .handler(ctx -> this.get(ctx, Database.Table.DISCOVERY_PROFILE));

        discoveryRouter.get("/:discovery_profile/credential")
                .handler(ctx -> this.get(ctx, Database.Table.DISCOVERY_CREDENTIAL));

        discoveryRouter.get("/")
                .handler(ctx -> this.list(ctx, Database.Table.DISCOVERY_PROFILE));

        discoveryRouter.get("/result/:discovery_profile")
                .handler(ctx -> this.get(ctx, Database.Table.DISCOVERY_RESULT));

        discoveryRouter.delete("/:discovery_profile/credential/:credential_profile")
                .handler(ctx -> this.delete(ctx, Database.Table.DISCOVERY_CREDENTIAL));

        discoveryRouter.post("/credential")
                .handler(ctx -> this.insert(ctx, Database.Table.DISCOVERY_CREDENTIAL));

        discoveryRouter.post("/")
                .handler(ctx -> this.insert(ctx, Database.Table.DISCOVERY_PROFILE));

        discoveryRouter.post("/run/:id")
                .handler(this::run);

        discoveryRouter.patch("/:id")
                .handler(ctx -> this.updateDiscovery(ctx, Database.Table.DISCOVERY_PROFILE));

        discoveryRouter.delete("/:id")
                .handler(ctx -> this.delete(ctx, Database.Table.DISCOVERY_PROFILE));

        router.route(Global.DISCOVERY_ENDPOINT).subRouter(discoveryRouter);
    }

    @Override
    public void insert(RoutingContext ctx, String tableName)
    {
        if (isInvalidDiscoveryInsertRequest(ctx)) return;

        var requestBody = ctx.body().asJsonObject();

        var discoveryRecord = new JsonObject()
                .put(Database.Discovery.IP, requestBody.getString(Database.Discovery.IP))
                .put(Database.Discovery.IP_TYPE, requestBody.getString(Database.Discovery.IP_TYPE))
                .put(Database.Discovery.NAME, requestBody.getString(Database.Discovery.NAME))
                .put(Database.Discovery.PORT, requestBody.getInteger(Database.Discovery.PORT));

        var discoveryInsertRequest = DbUtils.buildRequest(
                Database.Table.DISCOVERY_PROFILE,
                Database.Operation.INSERT,
                null,
                discoveryRecord,
                null
        );

        VERTX.eventBus().<JsonArray>request(Eventbus.EXECUTE_QUERY, discoveryInsertRequest, discoveryInsertReply ->
        {
            if (discoveryInsertReply.failed())
            {
                ApiUtils.sendFailure(ctx, 500, "Something went wrong","Failed to INSERT discovery. Cause: " + discoveryInsertReply.cause().getMessage());

                return;
            }

            var insertedDiscovery = discoveryInsertReply.result().body().getJsonObject(0);

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

            VERTX.eventBus().<JsonArray>request(Eventbus.EXECUTE_QUERY, credentialInsertRequest, credentialInsertReply ->
            {
                if (credentialInsertReply.failed())
                {
                    ApiUtils.sendFailure(ctx, 500, "Something went wrong",
                            "Failed to INSERT discovery-credentials. Cause: " + credentialInsertReply.cause().getMessage());

                    return;
                }

                ApiUtils.sendSuccess(ctx, 201, "Successfully inserted discovery", new JsonArray().add(new JsonObject().put(Database.Discovery.ID, discoveryId)));
            });
        });
    }

    public void updateDiscovery(RoutingContext ctx, String tableName)
    {
        if (isInvalidDiscoveryUpdateRequest(ctx)) return;

        this.update(ctx, tableName);
    }

    public void run(RoutingContext ctx)
    {
        var id = Validators.validateID(ctx);

        if(id == -1) ApiUtils.sendFailure(ctx, 400, "Please provide valid discovery id");

        VERTX.eventBus().send(Eventbus.RUN_DISCOVERY, new JsonObject().put(Database.Discovery.ID, id));

        ApiUtils.sendSuccess(ctx, 200, "Your discovery with id " + id + " is under process", new JsonArray());
    }

    /**
     * Helper method to validate discovery creation request
     * @param ctx Routing context
     * @return true if invalid
     */
    private boolean isInvalidDiscoveryInsertRequest(RoutingContext ctx)
    {
        if (Validators.validateBody(ctx)) return true;

        var requiredFields = new String[]{
                Database.Discovery.IP,
                Database.Discovery.IP_TYPE,
                Database.Discovery.NAME,
                Database.Discovery.PORT,
        };

        if (Validators.validateInputFields(ctx, requiredFields, true)) return true;

        var requestBody = ctx.body().asJsonObject();

        var credentialIds = requestBody.getJsonArray(Database.DiscoveryCredential.CREDENTIAL_PROFILE);

        if (credentialIds == null || credentialIds.isEmpty())
        {
            ApiUtils.sendFailure(ctx, 400, "Missing or empty 'credentials' field");

            return true;
        }

        for (var credentialId : credentialIds)
        {
            try
            {
                Integer.parseInt(credentialId.toString());
            }
            catch (NumberFormatException ex)
            {
                ApiUtils.sendFailure(ctx, 400, "Invalid credential ID: " + credentialId);
                return true;
            }
        }

        var port = requestBody.getInteger(Database.Discovery.PORT);

        if (port < 1 || port > 65535)
        {
            ApiUtils.sendFailure(ctx, 400, "Port must be between 1 and 65535");

            return true;
        }

        var ipType = requestBody.getString(Database.Discovery.IP_TYPE);

        if (!ipType.equals("SINGLE") && !ipType.equals("RANGE") && !ipType.equals("CIDR"))
        {
            ApiUtils.sendFailure(ctx, 400, "Invalid IP type. Only 'SINGLE', 'RANGE' and 'CIDR' are supported");

            return true;
        }

        var ip = requestBody.getString(Database.Discovery.IP);

        if (Validators.validateIpWithIpType(ip, ipType))
        {
            ApiUtils.sendFailure(ctx, 400, "Invalid IP or mismatch between IP and type: " + ip + ", " + ipType);

            return true;
        }

        return false;
    }

    /**
     * Helper method to validate discovery updation request
     * @param ctx Routing context
     * @return true if invalid
     */
    private boolean isInvalidDiscoveryUpdateRequest(RoutingContext ctx)
    {
        if(Validators.validateBody(ctx)) { return true; }

        if(Validators.validateInputFields(ctx, new String[]
                {
                        Database.Discovery.IP,
                        Database.Discovery.IP_TYPE,
                        Database.Discovery.NAME,
                        Database.Discovery.PORT
                }, false)) { return true; }

        if(! Validators.validatePort(ctx)) { return true; }

        var ipType = ctx.body().asJsonObject().getString(Database.Discovery.IP_TYPE);

        if (ipType != null && !ipType.equals("SINGLE") && !ipType.equals("RANGE") && !ipType.equals("CIDR"))
        {
            ApiUtils.sendFailure(ctx, 400, "Invalid IP type. Only 'SINGLE', 'RANGE' and 'CIDR' are supported");

            return true;
        }

        var ip = ctx.body().asJsonObject().getString(Database.Discovery.IP);

        if (ip != null && ipType != null && Validators.validateIpWithIpType(ip, ipType))
        {
            ApiUtils.sendFailure(ctx, 400, "Invalid IP address or mismatch Ip and IpType: " + ip + ", " + ipType);

            return true;
        }

        return false;
    }
}
