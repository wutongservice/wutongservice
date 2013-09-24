package com.borqs.server.wutong.notif;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.group.GroupImpl;
import com.borqs.server.wutong.group.GroupLogic;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class GroupApplyNotifSender extends GroupNotificationSender {
    private ArrayList<Long> toList = new ArrayList<Long>();

    public GroupApplyNotifSender() {
        super();
        isReplace = true;
    }

    @Override
    public List<Long> getScope(Context ctx, String senderId, RecordSet recs, Object... args) {
        List<Long> userIds = new ArrayList<Long>();
        String groupId = (String) args[0];
        String toIds = "";

        GroupLogic groupImpl = GlobalLogics.getGroup();
        toIds = groupImpl.canApproveUsers(ctx, Long.parseLong(groupId));

        if (StringUtils.isNotBlank(toIds))
            userIds = StringUtils2.splitIntList(toIds, ",");
        
        toList.addAll(userIds);

        return userIds;
    }

    @Override
    protected String getSettingKey(Context ctx) {
        return Constants.NTF_GROUP_APPLY;
    }

    @Override
    protected String getAppId(Context ctx, Object... args) {
        return (String) args[0];
    }

    @Override
    public String getTitle(Context ctx, String lang, Object... args) {
        String userName = (String) args[0];
        String groupType = (String) args[1];
        String groupName = (String) args[2];

        return userName + "申请加入" + groupType + "【" + groupName + "】";
    }

    @Override
    protected String getUri(Context ctx, Object... args) {
        return "borqs://circle/join_request?circleId=" + args[0];
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
        String userName = (String) args[0];
        String groupType = (String) args[1];
        String groupName = (String) args[2];
        String userId = (String) args[3];
        String groupId = (String) args[4];

        return "<a href=\"borqs://profile/details?uid=" + userId + "&tab=2\">" + userName + "</a>申请加入"
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
