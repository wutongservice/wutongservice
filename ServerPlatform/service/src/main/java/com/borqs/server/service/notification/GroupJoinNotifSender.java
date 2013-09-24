package com.borqs.server.service.notification;

import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class GroupJoinNotifSender extends GroupNotificationSender {
    public GroupJoinNotifSender(Platform p, Qiupu qiupu) {
        super(p, qiupu);
        isReplace = true;
    }

    @Override
    public List<Long> getScope(String senderId, Object... args) {
        List<Long> userIds = new ArrayList<Long>();
        String groupId = (String) args[0];

        try {
            String members = p.getGroupMembers(Long.parseLong(groupId));
            List<Long> memberIds = StringUtils2.splitIntList(members, ",");
            userIds.addAll(memberIds);

            if (userIds.contains(senderId))
                userIds.remove(senderId);
        } catch (Exception e) {

        }

        return userIds;
    }

    @Override
    protected String getSettingKey() {
        return Constants.NTF_GROUP_JOIN;
    }

    @Override
    protected String getAppId(Object... args) {
        return (String) args[0];
    }

    @Override
    public String getTitle(Object... args) {
        String displayNames = "";
        String groupId = (String) args[0];
        String groupType = (String) args[1];
        String groupName = (String) args[2];
        
        try {
            String memberIds = p.getMemberIds(Long.parseLong(groupId), 0, 4);
            RecordSet users = p.getUsers("", memberIds, "user_id, display_name");

            for (Record user : users) {
                String displayName = user.getString("display_name");

                displayNames += displayName + ", ";
            }

            if(StringUtils.isNotBlank(displayNames))
            {
                displayNames = StringUtils.substringBeforeLast(displayNames, ",");
            }

        } catch (Exception e) {

        }

        return displayNames + "加入了" + groupType + "【" + groupName + "】";
    }

    @Override
    protected String getUri(Object... args) {
        String groupId = (String) args[0];
        return "borqs://profile/details?uid=" + groupId + "&tab=2";
    }

    @Override
    protected String getTitleHtml(Object... args) {
        String nameLinks = "";
        String groupId = (String) args[0];
        String groupType = (String) args[1];
        String groupName = (String) args[2];

        try {
            String memberIds = p.getMemberIds(Long.parseLong(groupId), 0, 4);
            RecordSet users = p.getUsers("", memberIds, "user_id, display_name");

            List<String> l = new ArrayList<String>();
            for (Record user : users) {
                String userId = user.getString("user_id");
                String displayName = user.getString("display_name");

                l.add("<a href=\"borqs://profile/details?uid=" + userId + "&tab=2\">" + displayName + "</a>");
            }

            nameLinks = StringUtils.join(l, ", ");

        } catch (Exception e) {

        }

        return nameLinks + "加入了" + groupType
                + "【<a href=\"borqs://profile/details?uid=" + groupId + "&tab=2\">" + groupName + "</a>】";
    }

    @Override
    protected String getObjectId(Object... args) {
        return (String)args[0];
    }
}
