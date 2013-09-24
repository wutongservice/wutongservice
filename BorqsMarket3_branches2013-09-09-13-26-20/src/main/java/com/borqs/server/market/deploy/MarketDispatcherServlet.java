package com.borqs.server.market.deploy;


import com.borqs.server.market.utils.DateTimeUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class MarketDispatcherServlet extends DispatcherServlet {
    public static final long loadedAt = DateTimeUtils.nowMillis();

    public MarketDispatcherServlet() {
    }

    public MarketDispatcherServlet(WebApplicationContext webApplicationContext) {
        super(webApplicationContext);
    }


}
