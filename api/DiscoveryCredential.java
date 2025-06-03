package org.nms.api;

import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import org.nms.constants.DatabaseQueries;

import static org.nms.App.VERTX;

public class DiscoveryCredential implements AbstractHandler
{
    private static DiscoveryCredential instance;

    private DiscoveryCredential()
    {

    }

    public static DiscoveryCredential getInstance()
    {
        if(instance == null)
        {
            instance = new DiscoveryCredential();
        }

        return instance;
    }

    @Override
    public void init(Router router)
    {
        var discoveryCredentialRouter = Router.router(VERTX);

        var DISCOVERY_CREDENTIAL_ENDPOINT = "/api/v1/discovery-credential/*";


        router.route(DISCOVERY_CREDENTIAL_ENDPOINT).subRouter(discoveryCredentialRouter);
    }

    @Override
    public void insert(RoutingContext ctx)
    {

    }

    @Override
    public void update(RoutingContext ctx)
    {

    }

    @Override
    public void delete(RoutingContext ctx, String query)
    {

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