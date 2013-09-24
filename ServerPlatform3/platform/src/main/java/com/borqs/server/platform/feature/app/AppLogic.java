package com.borqs.server.platform.feature.app;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.logic.Logic;

public interface AppLogic extends Logic {
    boolean hasApp(Context ctx, int appId);
    App getApp(Context ctx, int appId);
}
