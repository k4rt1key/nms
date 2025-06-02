package org.nms.discovery;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClientOptions;
import org.nms.App;
import org.nms.constants.Database;
import org.nms.constants.Eventbus;
import org.nms.constants.Global;
import org.nms.constants.Plugin;
import org.nms.utils.DbUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Objects;

import static org.nms.App.LOGGER;

public class CredentialCheckService extends AbstractVerticle
{
    @Override
    public void start()
    {
        vertx.eventBus().localConsumer(Eventbus.CREDENTIAL_CHECK_START, this::credentialCheck);

        vertx.eventBus().localConsumer(Eventbus.SAVE_CREDENTIAL_CHECK, this::saveResults);
    }

    @Override
    public void stop()
    {
        LOGGER.info("CredentialCheckService Verticle Stopped");
    }

    public void credentialCheck(Message<JsonObject> message)
    {
        var discoveryId = message.body().getInteger(Database.DiscoveryResult.DISCOVERY_PROFILE);

        var ips = message.body().getJsonArray(Database.Discovery.IP);

        var port = message.body().getInteger(Database.Discovery.PORT);

        var credentials = new JsonArray();

        var queryRequest = DbUtils.buildRequest(
            Database.Table.DISCOVERY_CREDENTIAL,
            Database.Operation.GET,
            new JsonArray().add(
                    new JsonObject()
                            .put(Database.Common.COLUMN, Database.DiscoveryCredential.DISCOVERY_PROFILE)
                            .put(Database.Common.VALUE, discoveryId)
            ),
                null,
                null
        );

        vertx.eventBus().<JsonArray>request(Eventbus.EXECUTE_QUERY, queryRequest, dbResponse ->
        {
           if(dbResponse.succeeded() && !dbResponse.result().body().isEmpty())
           {
               var credentialIds = new JsonArray();

               var discoveryCredentials = dbResponse.result().body();

               for(var i = 0; i < discoveryCredentials.size(); i++)
               {
                   credentialIds.add(discoveryCredentials.getJsonObject(i).getInteger(Database.DiscoveryCredential.CREDENTIAL_PROFILE));
               }

               var getCredentials = new ArrayList<Future<Object>>();

               for(var i = 0; i < credentialIds.size(); i++)
               {
                   var getCredential = Promise.promise();

                   getCredentials.add(getCredential.future());

                   var getQueryRequest = DbUtils.buildRequest(
                           Database.Table.CREDENTIAL_PROFILE,
                           Database.Operation.GET,
                           new JsonArray().add(
                                   new JsonObject()
                                           .put(Database.Common.COLUMN, Database.Credential.ID)
                                           .put(Database.Common.VALUE, credentialIds.getInteger(i))
                           ),
                           null,
                           null
                   );

                   vertx.eventBus().<JsonArray>request(Eventbus.EXECUTE_QUERY, getQueryRequest, dbResponseForGet ->
                   {
                        if(dbResponseForGet.succeeded() && !dbResponseForGet.result().body().isEmpty())
                        {
                            credentials.add(dbResponseForGet.result().body().getJsonObject(0));
                        }
                        else
                        {
                            LOGGER.warn("Failed to GET credentials for discovery " + discoveryId);
                        }

                        getCredential.complete();

                   });
               }

               Future.join(getCredentials)
                               .onComplete(asyncResult ->
                               {
                                   LOGGER.debug("received credential check request for port: " + port + " ips: " + ips.encode());

                                   LOGGER.debug(credentials.encode());

                                   var pluginRequest = new JsonObject()
                                           .put(Plugin.DiscoveryRequest.TYPE, Plugin.DiscoveryRequest.DISCOVERY)
                                           .put(Plugin.DiscoveryRequest.DISCOVERY_PROFILE, discoveryId)
                                           .put(Plugin.DiscoveryRequest.PORT, port)
                                           .put(Plugin.DiscoveryRequest.IPS, ips)
                                           .put(Plugin.DiscoveryRequest.CREDENTIALS, credentials);

                                   vertx.eventBus().send(Eventbus.SPAWN_PLUGIN, pluginRequest);
                               });
           }
           else
           {
               LOGGER.warn("Cannot find credentials for discovery.id " + discoveryId);
           }
        });
    }

    public void saveResults(Message<JsonArray> message)
    {
        var pluginResponse = message.body();

        var dataToAdd = new JsonArray();

        for(var i = 0; i < pluginResponse.size(); i++)
        {
            dataToAdd.add(new JsonObject()
                    .put(Database.DiscoveryResult.DISCOVERY_PROFILE, pluginResponse.getJsonObject(i).getInteger(Plugin.DiscoveryResponse.DISCOVERY_PROFILE))
                    .put(Database.DiscoveryResult.IP, pluginResponse.getJsonObject(i).getString(Plugin.DiscoveryResponse.IP))
                    .put(Database.DiscoveryResult.MESSAGE, pluginResponse.getJsonObject(i).getString(Plugin.DiscoveryResponse.MESSAGE))
                    .put(Database.DiscoveryResult.CREDENTIAL_PROFILE, pluginResponse.getJsonObject(i).getJsonObject(Plugin.DiscoveryResponse.CREDENTIALS).getInteger(Database.Credential.ID))
                    .put(Database.DiscoveryResult.IP, pluginResponse.getJsonObject(i).getString(Plugin.DiscoveryResponse.IP))
                    .put(Database.DiscoveryResult.STATUS, pluginResponse.getJsonObject(i).getBoolean(Plugin.DiscoveryResponse.SUCCESS) ? Plugin.DiscoveryResponse.SUCCESS : Plugin.DiscoveryResponse.FAILED)
            );
        }

        var queryRequest = DbUtils.buildRequest(
          Database.Table.DISCOVERY_RESULT,
          Database.Operation.BATCH_INSERT,
          null,
          null,
          dataToAdd
        );

        if(!dataToAdd.isEmpty()) vertx.eventBus().send(Eventbus.EXECUTE_QUERY, queryRequest);
    }
}
