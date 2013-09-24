package com.borqs.server.service.notification;

import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.qiupu.Qiupu;

import java.util.List;

public abstract class GroupNotificationSender extends NotificationSender {
    public abstract List<Long> getScope(String senderId, Object... args);

    public GroupNotificationSender(Platform p, Qiupu qiupu) {
        super(p, qiupu);
    }

    @Override
    public String getTitle(Object... args)
    {
        return "";
    }

    @Override
    public String getBody(Object... args)
    {
        return "";
    }
}
