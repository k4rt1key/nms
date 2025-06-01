package org.nms.database;

import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import org.nms.App;
import org.nms.constants.Global;

public class PgClient
{
    private final SqlClient sqlClientInstance;

    private static PgClient instance;

    private PgClient()
    {
        var connectOptions = new PgConnectOptions()
                .setPort(Global.DB_PORT)
                .setHost(Global.DB_URL)
                .setDatabase(Global.DB_NAME)
                .setUser(Global.DB_USER)
                .setPassword(Global.DB_PASSWORD);

        var poolOptions = new PoolOptions().setMaxSize(Global.NUMBER_OF_DB_VERTICLE * 5);

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
