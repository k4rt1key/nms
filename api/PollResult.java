package org.nms.api;

import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.nms.constants.Database;
import org.nms.validators.Validators;
import org.nms.constants.Fields;
import org.nms.constants.Queries;
import org.nms.utils.DbUtils;

import static org.nms.App.VERTX;
import static org.nms.constants.Fields.Credential.*;
import static org.nms.constants.Fields.ENDPOINTS.CREDENTIALS_ENDPOINT;
import static org.nms.constants.Fields.ENDPOINTS.RESULT_ENDPOINT;
import static org.nms.utils.ApiUtils.sendFailure;
import static org.nms.utils.ApiUtils.sendSuccess;

public class PollResult implements BaseHandler
{
    private static PollResult instance;

    private PollResult()
    {

    }

    public static PollResult getInstance()
    {
        if(instance == null)
        {
            instance = new PollResult();
        }

        return instance;
    }

    @Override
    public void init(Router router)
    {
        var pollResultRouter = Router.router(VERTX);

        pollResultRouter.get("/")
                .handler((ctx) -> this.list(ctx, Database.Table.POLL_RESULTS));

        router.route(RESULT_ENDPOINT).subRouter(pollResultRouter);
    }

    @Override
    public void beforeInsert(RoutingContext ctx)
    {

    }

    @Override
    public void afterInsert(RoutingContext ctx)
    {

    }

    @Override
    public void beforeUpdate(RoutingContext ctx)
    {

    }

    @Override
    public void afterUpdate(RoutingContext ctx)
    {

    }

}