package org.nms.database;

import org.nms.constants.DatabaseConstant;
import org.nms.constants.DatabaseQueries;

import java.util.HashMap;

public class QueryStore
{
    private static QueryStore instance;

    private final HashMap<String, HashMap<String, String>> queryStore = new HashMap<>();

    private QueryStore() {}

    public static QueryStore getInstance()
    {
        if (instance == null)
        {
            instance = new QueryStore();

            instance.init();
        }

        return instance;
    }

    private void init()
    {
        var userQueries = new HashMap<String, String>();

        userQueries.put(DatabaseConstant.Operation.GET, DatabaseQueries.UserProfile.GET);

        userQueries.put(DatabaseConstant.Operation.GET_BY_USERNAME, DatabaseQueries.UserProfile.GET_BY_USERNAME);

        userQueries.put(DatabaseConstant.Operation.LIST, DatabaseQueries.UserProfile.LIST);

        userQueries.put(DatabaseConstant.Operation.INSERT, DatabaseQueries.UserProfile.INSERT);

        userQueries.put(DatabaseConstant.Operation.UPDATE, DatabaseQueries.UserProfile.UPDATE);

        userQueries.put(DatabaseConstant.Operation.DELETE, DatabaseQueries.UserProfile.DELETE);

        userQueries.put(DatabaseConstant.Operation.CREATE_SCHEMA, DatabaseQueries.UserProfile.CREATE_SCHEMA);

        var credentialQueries = new HashMap<String, String>();

        credentialQueries.put(DatabaseConstant.Operation.GET, DatabaseQueries.CredentialProfile.GET);

        credentialQueries.put(DatabaseConstant.Operation.LIST, DatabaseQueries.CredentialProfile.LIST);

        credentialQueries.put(DatabaseConstant.Operation.INSERT, DatabaseQueries.CredentialProfile.INSERT);

        credentialQueries.put(DatabaseConstant.Operation.UPDATE, DatabaseQueries.CredentialProfile.UPDATE);

        credentialQueries.put(DatabaseConstant.Operation.DELETE, DatabaseQueries.CredentialProfile.DELETE);

        credentialQueries.put(DatabaseConstant.Operation.CREATE_SCHEMA, DatabaseQueries.CredentialProfile.CREATE_SCHEMA);

        var discoveryQueries = new HashMap<String, String>();

        discoveryQueries.put(DatabaseConstant.Operation.GET, DatabaseQueries.DiscoveryProfile.GET);

        discoveryQueries.put(DatabaseConstant.Operation.LIST, DatabaseQueries.DiscoveryProfile.LIST);

        discoveryQueries.put(DatabaseConstant.Operation.INSERT, DatabaseQueries.DiscoveryProfile.INSERT);

        discoveryQueries.put(DatabaseConstant.Operation.UPDATE, DatabaseQueries.DiscoveryProfile.UPDATE);

        discoveryQueries.put(DatabaseConstant.Operation.DELETE, DatabaseQueries.DiscoveryProfile.DELETE);

        discoveryQueries.put(DatabaseConstant.Operation.CREATE_SCHEMA, DatabaseQueries.DiscoveryProfile.CREATE_SCHEMA);

        var discoveryCredentialQueries = new HashMap<String, String>();

        discoveryCredentialQueries.put(DatabaseConstant.Operation.GET, DatabaseQueries.DiscoveryCredential.INSERT);

        discoveryCredentialQueries.put(DatabaseConstant.Operation.DELETE, DatabaseQueries.DiscoveryCredential.DELETE);

        discoveryCredentialQueries.put(DatabaseConstant.Operation.CREATE_SCHEMA, DatabaseQueries.DiscoveryCredential.CREATE_SCHEMA);

        var discoveryResultQueries = new HashMap<String, String>();

        discoveryResultQueries.put(DatabaseConstant.Operation.GET, DatabaseQueries.DiscoveryResult.GET);

        discoveryResultQueries.put(DatabaseConstant.Operation.INSERT, DatabaseQueries.DiscoveryResult.INSERT);

        discoveryResultQueries.put(DatabaseConstant.Operation.DELETE, DatabaseQueries.DiscoveryResult.DELETE);

        discoveryResultQueries.put(DatabaseConstant.Operation.CREATE_SCHEMA, DatabaseQueries.DiscoveryResult.CREATE_SCHEMA);

        var monitorQueries = new HashMap<String, String>();

        monitorQueries.put(DatabaseConstant.Operation.GET, DatabaseQueries.Monitor.GET);

        monitorQueries.put(DatabaseConstant.Operation.LIST, DatabaseQueries.Monitor.LIST);

        monitorQueries.put(DatabaseConstant.Operation.INSERT, DatabaseQueries.Monitor.INSERT);

        monitorQueries.put(DatabaseConstant.Operation.DELETE, DatabaseQueries.Monitor.DELETE);

        monitorQueries.put(DatabaseConstant.Operation.CREATE_SCHEMA, DatabaseQueries.Monitor.CREATE_SCHEMA);

        var metricQueries = new HashMap<String, String>();

        metricQueries.put(DatabaseConstant.Operation.UPDATE, DatabaseQueries.Metric.UPDATE);

        metricQueries.put(DatabaseConstant.Operation.CREATE_SCHEMA, DatabaseQueries.Metric.CREATE_SCHEMA);

        var pollingResultsQueries = new HashMap<String, String>();

        pollingResultsQueries.put(DatabaseConstant.Operation.LIST, DatabaseQueries.PollingResult.LIST);

        pollingResultsQueries.put(DatabaseConstant.Operation.CREATE_SCHEMA, DatabaseQueries.PollingResult.CREATE_SCHEMA);

        queryStore.put(DatabaseConstant.SchemaName.USER_PROFILE, userQueries);

        queryStore.put(DatabaseConstant.SchemaName.CREDENTIAL_PROFILE, credentialQueries);

        queryStore.put(DatabaseConstant.SchemaName.DISCOVERY_PROFILE, discoveryQueries);

        queryStore.put(DatabaseConstant.SchemaName.DISCOVERY_CREDENTIAL, discoveryCredentialQueries);

        queryStore.put(DatabaseConstant.SchemaName.DISCOVERY_RESULT, discoveryResultQueries);

        queryStore.put(DatabaseConstant.SchemaName.MONITOR, monitorQueries);

        queryStore.put(DatabaseConstant.SchemaName.METRIC, metricQueries);

        queryStore.put(DatabaseConstant.SchemaName.POLLING_RESULT, pollingResultsQueries);
    }

    public String getQuery(String schemaName, String operation)
    {
        return queryStore.get(schemaName).get(operation);
    }
}