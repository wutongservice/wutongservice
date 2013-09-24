package com.borqs.server.service.notification;

import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class GroupInviteNotifSender extends GroupNotificationSender {
    private static final org.slf4j.Logger L = LoggerFactory.getLogger(GroupInviteNotifSender.class);

    public GroupInviteNotifSender(Platform p, Qiupu qiupu) {
        super(p, qiupu);
        isReplace = true;
    }

    @Override
    public List<Long> getScope(String senderId, Object... args) {
        String toInviteIds = (String) args[0];
        List<Long> userIds = StringUtils2.splitIntList(toInviteIds, ",");

        HashSet<Long> set = new HashSet<Long>(userIds);
        userIds = new ArrayList<Long>(set);
        //exclude sender
        if(StringUtils.isNotBlank(senderId))
        {
            userIds.remove(Long.parseLong(senderId));
        }

        L.trace("Group Invite Notification Receive userIds: " + userIds);
        return userIds;
    }

    @Override
    protected String getSettingKey() {
        return Constants.NTF_GROUP_INVITE;
    }

    @Override
    protected String getAppId(Object... args) {
        return (String) args[0];
    }

    @Override
    public String getTitle(Object... args) {
        String source = (String) args[0];
        String groupType = (String) args[1];
        String groupName = (String) args[2];
        String action = (String) args[3];
        
        return source + action + "您加入" + groupType + "【" + groupName + "】";
    }

    @Override
    protected String getUri(Object... args) {
        return "borqs://circle/invitation?circleId=" + args[0];
    }

    @Override
    protected String getTitleHtml(Object... args) {
        String source = (String) args[0];
        String groupType = (String) args[1];
        String groupName = (String) args[2];
        String sourceId = (String) args[3];
        String groupId = (String) args[4];
        String action = (String) args[5];
        
        return "<a href=\"borqs://profile/details?uid=" + sourceId + "&tab=2\">" + source + "</a>" + action + "您加入"
                + groupType + "【<a href=\"borqs://profile/details?uid=" + groupId + "&tab=2\">" + groupName + "</a>】";
    }

    @Override
    public String getBody(Object... args)
    {
        return (String)args[0];
    }

    @Override
    protected String getObjectId(Object... args) {
        return (String)args[0];
    }
}
