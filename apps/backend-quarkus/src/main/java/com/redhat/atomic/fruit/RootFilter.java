package com.redhat.atomic.fruit;

import io.vertx.core.http.HttpMethod;
import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;

public class RootFilter {
    @RouteFilter(400)
    void rootDirector(RoutingContext rc) {
        String uri = rc.request().uri();
        
        if (uri.equals("/") && rc.request().method() == HttpMethod.GET) {
            rc.reroute("/index.html");
            return;
        }
        
        rc.next();
    }
}
