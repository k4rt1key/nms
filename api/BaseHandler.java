package org.nms.api;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public interface BaseHandler
{
    void init(Router router);

    void list(RoutingContext ctx);

    void get(RoutingContext ctx);

    void insert(RoutingContext ctx);

    void update(RoutingContext ctx);

    void delete(RoutingContext ctx);
}
