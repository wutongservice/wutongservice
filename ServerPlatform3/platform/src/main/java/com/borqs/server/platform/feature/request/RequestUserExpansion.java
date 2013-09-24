package com.borqs.server.platform.feature.request;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.UserExpansion;
import com.borqs.server.platform.feature.account.Users;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;

import java.util.Map;

public class RequestUserExpansion implements UserExpansion {

    public static final String COL_PENDING_REQ_TYPES = "pending_req_types";
    public static final String COL_PENDING_REQUESTS = "pending_requests";

    private RequestLogic requestLogic;

    public RequestUserExpansion() {
    }

    public RequestUserExpansion(RequestLogic requestLogic) {
        this.requestLogic = requestLogic;
    }

    public RequestLogic getRequestLogic() {
        return requestLogic;
    }

    public void setRequestLogic(RequestLogic requestLogic) {
        this.requestLogic = requestLogic;
    }

    @Override
    public void expand(Context ctx, String[] expCols, Users users) {
        if (!ctx.isLogined())
            return;

        if (!ArrayUtils.contains(expCols, COL_PENDING_REQ_TYPES))
            return;

        if (CollectionUtils.isEmpty(users))
            return;

        Map<Long, int[]> m = requestLogic.getPendingTypes(ctx, ctx.getViewer(), users.getUserIds());
        for (Map.Entry<Long, int[]> entry : m.entrySet()) {
            long userId = entry.getKey();
            int[] types = entry.getValue();

            User user = users.getUser(userId);
            user.setAddon(COL_PENDING_REQ_TYPES, types);
        }
    }
}
