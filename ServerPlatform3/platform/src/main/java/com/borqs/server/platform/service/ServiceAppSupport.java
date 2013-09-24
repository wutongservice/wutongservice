package com.borqs.server.platform.service;


import com.borqs.server.platform.app.AppMain;
import com.borqs.server.platform.log.Logger;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

public abstract class ServiceAppSupport implements AppMain {
    private static final Logger L = Logger.get(ServiceApp.class);

    private List<Service> services;

    protected ServiceAppSupport() {
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

    @Override
    public void run(String[] args) {
        try {
            if (CollectionUtils.isNotEmpty(services)) {
                for (Service service : services)
                    service.start();
            }

            loop();
        } catch (Throwable t) {
            L.error(null, "Run services error", t);
        } finally {
            if (CollectionUtils.isNotEmpty(services)) {
                for (Service service : services)
                    service.stop();
            }
        }
    }

    protected abstract void loop();
}
