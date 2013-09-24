package com.borqs.server.platform.feature.opline;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.logic.Logic;

import java.util.Map;

public interface OpLineLogic extends Logic {
    void appends(Context ctx, Operation... opers);

    Operations getOperationsBefore(Context ctx, long userId, long beforeOperId, int count);

    Operation getLastOperation(Context ctx, long userId, int[] actions);
    Operations getOpsWithFlag(Context ctx, long userId, int[] actions, int flag, long minTime);
    Operations getOpsWithFlagByInterval(Context ctx, long userId, int[] actions, int flag, long maxInterval, long minTime);

    void setFlag(Context ctx, int flag, long... operIds);
}
