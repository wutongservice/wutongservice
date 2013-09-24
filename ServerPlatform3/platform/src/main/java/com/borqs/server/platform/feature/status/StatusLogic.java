package com.borqs.server.platform.feature.status;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.logic.Logic;

import java.util.Map;

public interface StatusLogic extends Logic {
    Status getStatus(Context ctx, long userId);
    Map<Long, Status> getStatuses(Context ctx, long... userIds);
    void updateStatus(Context ctx, Status status);
}
