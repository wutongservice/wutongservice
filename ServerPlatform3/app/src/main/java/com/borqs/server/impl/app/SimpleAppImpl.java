package com.borqs.server.impl.app;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.app.App;
import com.borqs.server.platform.feature.app.AppLogic;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

public class SimpleAppImpl implements AppLogic {

    private List<App> apps;

    public SimpleAppImpl() {
    }

    public List<App> getApps() {
        return apps;
    }

    public void setApps(List<App> apps) {
        this.apps = apps;
    }

    @Override
    public boolean hasApp(Context ctx, int appId) {
        return getApp(ctx, appId) != null;
    }

    @Override
    public App getApp(Context ctx, int appId) {
        if (CollectionUtils.isEmpty(apps))
            return null;

        for (App app : apps) {
            if (app.getAppId() == appId)
                return app;
        }
        return null;
    }
}
