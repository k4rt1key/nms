package org.nms.database;

import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import org.nms.App;
import org.nms.constants.Config;

public class PgClient
{
    private final SqlClient sqlClientInstance;

    private static PgClient instance;

    private PgClient()
    {
        var connectOptions = new PgConnectOptions()
                .setPort(Config.DB_PORT)
                .setHost(Config.DB_URL)
                .setDatabase(Config.DB_NAME)
                .setUser(Config.DB_USER)
                .setPassword(Config.DB_PASSWORD);

        var poolOptions = new PoolOptions().setMaxSize(5);

        this.sqlClientInstance = PgBuilder
                .client()
                .with(poolOptions)
                .connectingTo(connectOptions)
                .using(App.vertx)
                .build();
    }

    // Static method to get the singleton instance
    public static PgClient getInstance()
    {
        if (instance == null) {
            instance = new PgClient();
        }
        return instance;
    }

    // Method to access the SQL client
    public SqlClient getSqlClient()
    {
        return this.sqlClientInstance;
    }
}