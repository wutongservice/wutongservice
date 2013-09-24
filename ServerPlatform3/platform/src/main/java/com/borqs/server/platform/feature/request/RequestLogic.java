package com.borqs.server.platform.feature.request;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.logic.Logic;

import java.util.Map;

public interface RequestLogic extends Logic {
    void create(Context ctx, Request... requests);
    void done(Context ctx, long... requestIds);
    
    Requests getAllRequests(Context ctx, long toId, int app, int type, int limit);
    Requests getPendingRequests(Context ctx, long toId, int app, int type);
    Requests getDoneRequests(Context ctx, long toId, int app, int type, int limit);
    
    long getPendingCount(Context ctx, long toId, int app, int type);

    Map<Long, int[]> getPendingTypes(Context ctx, long fromId, long... toIds);
}
