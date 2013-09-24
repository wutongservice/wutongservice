package com.borqs.server.wutong.notif;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.RecordSet;

import java.util.List;
import java.util.Map;

public abstract class GroupNotificationSender extends NotificationSender {
    @Override
    public List<Long> getScope(Context ctx, String senderId, Object... args) {
        return getScope(ctx, senderId, null, args);
    }

    public abstract List<Long> getScope(Context ctx, String senderId, RecordSet recs, Object... args);

    public GroupNotificationSender() {
        super();
    }

    @Override
    public String getTitle(Context ctx, String lang, Object... args)
    {
        return "";
    }

    @Override
    public String getBody(Context ctx, Object... args)
    {
        return "";
    }
}
