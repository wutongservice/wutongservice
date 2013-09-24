package com.borqs.server.platform.web;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.service.Service;
import com.borqs.server.platform.util.Initializable;
import com.borqs.server.platform.util.NetAddress;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JettyService implements Service, Initializable {
    private Server server;

    private Set<String> addresses;
    private List<WebApp> apps;

    public Set<String> getAddresses() {
        return addresses;
    }

    public void setAddresses(Set<String> addresses) {
        this.addresses = addresses;
    }

    public List<WebApp> getApps() {
        return apps;
    }

    public void setApps(List<WebApp> apps) {
        this.apps = apps;
    }

    @Override
    public void init() {
        server = new Server();
        bindAddress();
        initWebApps();
    }

    @Override
    public void destroy() {
        server.destroy();
        server = null;
    }

    private void bindAddress() {
        Set<String> addrs = CollectionUtils.isNotEmpty(addresses) ? addresses : new HashSet<String>();
        if (addrs.isEmpty())
            addrs.add("*:8080");

        for (Object addr : addrs) {
            NetAddress bindAddr = NetAddress.parse(ObjectUtils.toString(addr));
            Connector connector = new SelectChannelConnector();
            if (bindAddr.host != null)
                connector.setHost(bindAddr.host);

            connector.setPort(bindAddr.port);
            server.addConnector(connector);
        }
    }

    private void initWebApps() {
        if (CollectionUtils.isEmpty(apps))
            return;

        HandlerList hl = new HandlerList();
        for (WebApp app : apps) {
            if (app == null)
                continue;
            WebAppContext ctx = new WebAppContext();
            app.setup(ctx);
            hl.addHandler(ctx);
        }
        server.setHandler(hl);
    }

    @Override
    public void start() {
        try {
            server.start();
        } catch (Exception e) {
            throw new ServerException(E.WEB_SERVER, e, "Start jetty server error");
        }
    }

    @Override
    public void stop() {
        try {
            server.stop();
            server.join();
        } catch (Exception e) {
            throw new ServerException(E.WEB_SERVER, e, "Stop jetty server error");
        }
    }

    @Override
    public boolean isStarted() {
        return server != null && server.isStarted();
    }
}
