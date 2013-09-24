package com.borqs.server.wutong.notif;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.Constants;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class GroupInviteNotifSender extends GroupNotificationSender {
    private static final Logger L = Logger.getLogger(GroupInviteNotifSender.class);
    private ArrayList<Long> toList = new ArrayList<Long>();

    public GroupInviteNotifSender() {
        super();
        isReplace = true;
    }

    @Override
    public List<Long> getScope(Context ctx, String senderId, RecordSet recs, Object... args) {
        String toInviteIds = (String) args[0];
        List<Long> userIds = StringUtils2.splitIntList(toInviteIds, ",");

        HashSet<Long> set = new HashSet<Long>(userIds);
        userIds = new ArrayList<Long>(set);
        //exclude sender
        if(StringUtils.isNotBlank(senderId))
        {
            userIds.remove(Long.parseLong(senderId));
        }

        L.trace(ctx, "Group Invite Notification Receive userIds: " + userIds);
        toList.addAll(userIds);
        return userIds;
    }

    @Override
    protected String getSettingKey(Context ctx) {
        return Constants.NTF_GROUP_INVITE;
    }

    @Override
    protected String getAppId(Context ctx, Object... args) {
        return (String) args[0];
    }

    @Override
    public String getTitle(Context ctx, String lang, Object... args) {
        String source = (String) args[0];
        String groupType = (String) args[1];
        String groupName = (String) args[2];
        String action = (String) args[3];
        
        return source + action + "您加入" + groupType + "【" + groupName + "】";
    }

    @Override
    protected String getUri(Context ctx, Object... args) {
        return "borqs://circle/invitation?circleId=" + args[0];
    }

    @Override
    protected String getData(Context ctx, Object... args) {
        if (toList.isEmpty())
            return "";
        else
            return "," + StringUtils2.joinIgnoreBlank(",", toList) + ",";
    }

    @Override
    protected String getTitleHtml(Context ctx, String lang, Object... args) {
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
    public String getBody(Context ctx, Object... args)
    {
        return (String)args[0];
    }

    @Override
    protected String getObjectId(Context ctx, Object... args) {
        return (String)args[0];
    }
}
