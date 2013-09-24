package com.borqs.server.platform.feature.status;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.UserExpansion;
import com.borqs.server.platform.feature.account.Users;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;

import java.util.Map;

public class StatusUserExpansion implements UserExpansion {

    public static final String COL_STATUS = "status";
    public static final String COL_STATUS_UPDATED_TIME = "status_updated_time";

    public static final String[] EXPAND_COLUMNS = {COL_STATUS, COL_STATUS_UPDATED_TIME};

    static {
        User.registerColumnsAlias("@xstatus,#xstatus", EXPAND_COLUMNS);
    }

    private StatusLogic status;

    public StatusUserExpansion() {
    }

    public StatusUserExpansion(StatusLogic status) {
        this.status = status;
    }

    public StatusLogic getStatus() {
        return status;
    }

    public void setStatus(StatusLogic status) {
        this.status = status;
    }

    @Override
    public void expand(Context ctx, String[] expCols, Users data) {
        if (CollectionUtils.isEmpty(data))
            return;

        boolean expandStatus = expCols == null || ArrayUtils.contains(expCols, COL_STATUS);
        boolean expandStatusUpdatedTime = expCols == null || ArrayUtils.contains(expCols, COL_STATUS_UPDATED_TIME);
        if (!expandStatus && !expandStatusUpdatedTime)
            return;

        long[] userIds = data.getUserIds();
        Map<Long, Status> sm = status.getStatuses(ctx, userIds);
        for (User user : data) {
            if (user == null)
                continue;

            Status st = sm.get(user.getUserId());

            if (expandStatus)
                user.setAddon(COL_STATUS, st != null ? ObjectUtils.toString(st.status) : "");

            if (expandStatusUpdatedTime)
                user.setAddon(COL_STATUS_UPDATED_TIME, st != null ? st.updatedTime : 0L);
        }
    }
}
