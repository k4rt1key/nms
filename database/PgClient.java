package org.nms.database;

import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import org.nms.App;
import org.nms.constants.Configuration;

public class PgClient
{
    private final SqlClient sqlClientInstance;

    private static PgClient instance;

    private PgClient()
    {
        var connectOptions = new PgConnectOptions()
                .setPort(Configuration.DB_PORT)
                .setHost(Configuration.DB_URL)
                .setDatabase(Configuration.DB_NAME)
                .setUser(Configuration.DB_USER)
                .setPassword(Configuration.DB_PASSWORD);

        var poolOptions = new PoolOptions().setMaxSize(Configuration.NUMBER_OF_DB_VERTICLE * 5);

        this.sqlClientInstance = PgBuilder
                .client()
                .with(poolOptions)
                .connectingTo(connectOptions)
                .using(App.VERTX)
                .build();
    }

    public static PgClient getInstance()
    {
        if (instance == null)
        {
            instance = new PgClient();
        }

        return instance;
    }

    public SqlClient getSqlClient()
    {
        return this.sqlClientInstance;
    }
}
