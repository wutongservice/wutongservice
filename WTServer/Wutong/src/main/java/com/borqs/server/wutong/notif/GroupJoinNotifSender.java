package com.borqs.server.wutong.notif;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.account2.AccountImpl;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.group.GroupImpl;
import com.borqs.server.wutong.group.GroupLogic;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupJoinNotifSender extends GroupNotificationSender {
    private String senderId = "";

    public GroupJoinNotifSender() {
        super();
        isReplace = true;
    }

    @Override
    public List<Long> getScope(Context ctx, String senderId, RecordSet recs, Object... args) {
        List<Long> userIds = new ArrayList<Long>();
        String groupId = (String) args[0];

        try {
            GroupLogic groupImpl = GlobalLogics.getGroup();
            String members = groupImpl.getAllMembers(ctx, Long.parseLong(groupId), -1, -1, "");
            List<Long> memberIds = StringUtils2.splitIntList(members, ",");
            userIds.addAll(memberIds);

            HashSet<Long> set = new HashSet<Long>(userIds);
            userIds = new ArrayList<Long>(set);

            //exclude sender
            if(StringUtils.isNotBlank(senderId))
            {
                userIds.remove(Long.parseLong(senderId));
            }

            if (recs == null) {
                recs = new RecordSet();
            }
            RecordSet notifRecs = groupImpl.getMembersNotification(ctx, Long.parseLong(groupId), members);
            recs.addAll(notifRecs);

            for (Record notifRec : notifRecs) {
                String memberId = notifRec.getString("member");
                long recvNotif = notifRec.getInt("recv_notif", 0);
                if (recvNotif == 2) {
                    userIds.remove(Long.parseLong(memberId));
                }
            }

            AccountLogic account = GlobalLogics.getAccount();
            this.senderId = senderId;
        } catch (Exception e) {

        }

        return userIds;
    }

    @Override
    protected String getSettingKey(Context ctx) {
        return Constants.NTF_GROUP_JOIN;
    }

    @Override
    protected String getAppId(Context ctx, Object... args) {
        return (String) args[0];
    }

    @Override
    public String getTitle(Context ctx, String lang, Object... args) {
        String displayNames = "";
        String groupId = (String) args[0];
        String groupType = (String) args[1];
        String groupName = (String) args[2];
        
        try {
            GroupLogic groupImpl = GlobalLogics.getGroup();
            String memberIds = groupImpl.getAllMembers(ctx, Long.parseLong(groupId), 0, 4, "");
            if (!StringUtils.contains(memberIds, this.senderId)) {
                memberIds = this.senderId + "," + memberIds;
            }
            AccountLogic account = GlobalLogics.getAccount();
            RecordSet users = account.getUsers(ctx, "", memberIds, "user_id, display_name");

            Record rec = new Record();
            for (Record user: users) {
                rec.put(user.getString("user_id"), user.getString("display_name"));
            }

            Set<String> members = StringUtils2.splitSet(memberIds, ",", true);
            for (String member : members) {
                String displayName = rec.getString(member);

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
    protected String getUri(Context ctx, Object... args) {
        String groupId = (String) args[0];
        return "borqs://profile/details?uid=" + groupId + "&tab=2";
    }

    @Override
    protected String getTitleHtml(Context ctx, String lang, Object... args) {
        String nameLinks = "";
        String groupId = (String) args[0];
        String groupType = (String) args[1];
        String groupName = (String) args[2];

        try {
            GroupLogic groupImpl = GlobalLogics.getGroup();
            String memberIds = groupImpl.getAllMembers(ctx, Long.parseLong(groupId), 0, 4, "");
            if (!StringUtils.contains(memberIds, this.senderId)) {
                memberIds = this.senderId + "," + memberIds;
            }
            AccountLogic account = GlobalLogics.getAccount();
            RecordSet users = account.getUsers(ctx, "", memberIds, "user_id, display_name");

            Record rec = new Record();
            for (Record user: users) {
                rec.put(user.getString("user_id"), user.getString("display_name"));
            }

            Set<String> members = StringUtils2.splitSet(memberIds, ",", true);
            List<String> l = new ArrayList<String>();
            for (String member : members) {
                String displayName = rec.getString(member);

                l.add("<a href=\"borqs://profile/details?uid=" + member + "&tab=2\">" + displayName + "</a>");
            }

            nameLinks = StringUtils.join(l, ", ");

        } catch (Exception e) {

        }

        return nameLinks + "加入了" + groupType
                + "【<a href=\"borqs://profile/details?uid=" + groupId + "&tab=2\">" + groupName + "</a>】";
    }

    @Override
    protected String getObjectId(Context ctx, Object... args) {
        return (String)args[0];
    }
}
