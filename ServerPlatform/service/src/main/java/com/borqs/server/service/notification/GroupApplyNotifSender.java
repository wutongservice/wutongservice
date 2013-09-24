package com.borqs.server.service.notification;

import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class GroupApplyNotifSender extends GroupNotificationSender {

    public GroupApplyNotifSender(Platform p, Qiupu qiupu) {
        super(p, qiupu);
        isReplace = true;
    }

    @Override
    public List<Long> getScope(String senderId, Object... args) {
        List<Long> userIds = new ArrayList<Long>();
        String groupId = (String) args[0];
        String toIds = "";
        try {
            toIds = p.canApproveUsers(Long.parseLong(groupId));
        } catch (AvroRemoteException e) {

        }
        if (StringUtils.isNotBlank(toIds))
            userIds = StringUtils2.splitIntList(toIds, ",");
        
        return userIds;
    }

    @Override
    protected String getSettingKey() {
        return Constants.NTF_GROUP_APPLY;
    }

    @Override
    protected String getAppId(Object... args) {
        return (String) args[0];
    }

    @Override
    public String getTitle(Object... args) {
        String userName = (String) args[0];
        String groupType = (String) args[1];
        String groupName = (String) args[2];

        return userName + "申请加入" + groupType + "【" + groupName + "】";
    }

    @Override
    protected String getUri(Object... args) {
        return "borqs://circle/join_request?circleId=" + args[0];
    }

    @Override
    protected String getTitleHtml(Object... args) {
        String userName = (String) args[0];
        String groupType = (String) args[1];
        String groupName = (String) args[2];
        String userId = (String) args[3];
        String groupId = (String) args[4];

        return "<a href=\"borqs://profile/details?uid=" + userId + "&tab=2\">" + userName + "</a>申请加入"
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
