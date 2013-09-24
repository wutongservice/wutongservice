package com.borqs.server.wutong.notif;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.conversation.ConversationImpl;
import com.borqs.server.wutong.conversation.ConversationLogic;
import com.borqs.server.wutong.group.GroupImpl;
import com.borqs.server.wutong.group.GroupLogic;
import com.borqs.server.wutong.ignore.IgnoreImpl;
import com.borqs.server.wutong.ignore.IgnoreLogic;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SharedAppNotifSender extends GroupNotificationSender {

    private ArrayList<Long> groups = new ArrayList<Long>();
    private HashSet<Long> toIds = new HashSet<Long>();

    public SharedAppNotifSender() {
        super();
    }
    
    public List<Long> getScope(Context ctx, String senderId, RecordSet recs, Object... args) {
        List<Long> userIds = new ArrayList<Long>();
        if (getSettingKey(ctx).equals(Constants.NTF_APP_SHARE)) {
                /*
                Record es_shared_Users = p.getPosts((String) args[0], "mentions").getFirstRecord();
                List<String> toUser = StringUtils2.splitList(es_shared_Users.getString("mentions"), ",", true);
                for (String u : toUser) {
                    if (!userIds.contains(Long.parseLong(u)) && !u.equals("0") && !u.equals("")) {
                        userIds.add(Long.parseLong(u));
                    }
                }
                */
                //=========================new send to ,from conversation=========================
                List<String> reasons = new ArrayList<String>();
                reasons.add(String.valueOf(Constants.C_STREAM_TO));
                reasons.add(String.valueOf(Constants.C_STREAM_ADDTO));
            ConversationLogic conversation = GlobalLogics.getConversation();
            RecordSet conversation_users = conversation.getConversation(ctx, Constants.POST_OBJECT, (String) args[0], reasons, 0, 0, 100);
                for (Record r : conversation_users) {
                    long userId = Long.parseLong(r.getString("from_"));
                    if (!userIds.contains(userId)) {
                        if ((userId >= Constants.PUBLIC_CIRCLE_ID_BEGIN)
                                && (userId <= Constants.GROUP_ID_END)) {
                            groups.add(userId);
                            GroupLogic groupImpl = GlobalLogics.getGroup();
                            String members = groupImpl.getAllMembers(ctx, userId, -1, -1, "");
                            List<Long> memberIds = StringUtils2.splitIntList(members, ",");
                            userIds.addAll(memberIds);

                            if (recs == null) {
                                recs = new RecordSet();
                            }
                            RecordSet notifRecs = groupImpl.getMembersNotification(ctx, userId, members);
                            recs.addAll(notifRecs);

                            for (Record notifRec : notifRecs) {
                                String memberId = notifRec.getString("member");
                                long recvNotif = notifRec.getInt("recv_notif", 0);
                                if (recvNotif == 2) {
                                    userIds.remove(Long.parseLong(memberId));
                                }
                            }
                        }
                        else {
                            userIds.add(userId);
                            toIds.add(userId);
                        }
                    }
                    else {
                        if (Constants.getUserTypeById(userId) == Constants.USER_OBJECT) {
                            toIds.add(userId);
                        }
                    }
                }
                //=========================new send to ,from conversation end ====================
        }
        if (userIds.contains(Long.parseLong((String)args[1])))
            userIds.remove(Long.parseLong((String)args[1]));

        HashSet<Long> set = new HashSet<Long>(userIds);
        userIds = new ArrayList<Long>(set);
        //exclude sender
        if(StringUtils.isNotBlank(senderId))
        {
        	userIds.remove(Long.parseLong(senderId));
        }
        try {
            IgnoreLogic ignore = GlobalLogics.getIgnore();
            userIds = ignore.formatIgnoreUserListP(ctx, userIds, "", "");
        } catch (Exception e) {
        }
        return userIds;
    }
    
   @Override
    protected String getSettingKey(Context ctx) {
        return Constants.NTF_APP_SHARE;
    }

    @Override
    protected String getAppId(Context ctx, Object... args) {
//        return String.valueOf(Constants.APP_TYPE_QIUPU);
        return (String) args[0];
    }

    @Override
    public String getTitle(Context ctx, String lang, Object... args) {
        String apk_id = (String)args[0];
        String userid = (String)args[1];
        String username = (String)args[2];
        String appname = (String)args[3];
        String returnString = "";

        boolean isEn = StringUtils.contains(lang, "en") ? true : false;
        String sType = Constants.getBundleStringByLang(lang, "file.share.notif.app");

        if (getSettingKey(ctx).equals(Constants.NTF_APP_SHARE)) {
            //returnString = "您的朋友<user id="+userid+">"+username+"</user>给您分享了应用 <apk id="+apk_id+">"+appname+"</apk>";
            if (groups.isEmpty()) {
                if (isEn)
                    returnString = username + " " + Constants.getBundleStringByLang(lang, "other.share.notif.title") + " " + sType;
                else
                    returnString = username + Constants.getBundleStringByLang(lang, "other.share.notif.title") + sType;
            }
            else {
                String groupIds = StringUtils2.joinIgnoreBlank(",", groups);
                String groupType = "公共圈子";
                RecordSet recs = new RecordSet();

                GroupLogic groupImpl = GlobalLogics.getGroup();
                groupType = groupImpl.getGroupTypeStr(ctx, lang, groups.get(0));
                    recs = groupImpl.getSimpleGroups(ctx, Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.GROUP_ID_END, groupIds, Constants.GRP_COL_NAME);

                ArrayList<String> groupNames = new ArrayList<String>();
                for (Record rec : recs) {
                    String groupName = rec.getString(Constants.GRP_COL_NAME);
                    groupNames.add("【" + groupName + "】");
                }
                if (isEn)
                    returnString = username + " " + Constants.getBundleStringByLang(lang, "other.share.notif.group.title") + " " + sType
                            + " " +Constants.getBundleStringByLang(lang, "other.share.notif.from") + " " + groupType + StringUtils2.joinIgnoreBlank("，", groupNames);
                else
                    returnString = username + Constants.getBundleStringByLang(lang, "other.share.notif.from") + groupType + StringUtils2.joinIgnoreBlank("，", groupNames)
                            + Constants.getBundleStringByLang(lang, "other.share.notif.group.title") + sType;
            }
        }
        return returnString;
    }

    @Override
    protected String getAction(Context ctx, Object... args) {
        return "android.intent.action.VIEW";
    }

    @Override
    protected String getUri(Context ctx, Object... args) {
        String apk_id = (String)args[0];
        return "borqs://application/details?id="+apk_id+"";
    }

    @Override
    protected String getData(Context ctx, Object... args) {
        if (toIds.isEmpty())
            return "";
        else
            return "," + StringUtils2.joinIgnoreBlank(",", toIds) + ",";
    }
    
    @Override
    protected String getTitleHtml(Context ctx, String lang, Object... args) {
        String apk_id = (String)args[0];
        String userid = (String)args[1];
        String username = (String)args[2];
        String appname = (String)args[3];
        String returnString = "";

        boolean isEn = StringUtils.contains(lang, "en") ? true : false;
        String sType = Constants.getBundleStringByLang(lang, "file.share.notif.app");

        if (getSettingKey(ctx).equals(Constants.NTF_APP_SHARE)) {
            //returnString = "您的朋友<user id="+userid+">"+username+"</user>给您分享了应用 <apk id="+apk_id+">"+appname+"</apk>";
            if (groups.isEmpty()) {
                if (isEn)
                    returnString = "<a href=\"borqs://profile/details?uid=" + userid + "&tab=2\">" + username + "</a>"
                            + " " + Constants.getBundleStringByLang(lang, "other.share.notif.title") + " " + sType;
                else
                    returnString = "<a href=\"borqs://profile/details?uid=" + userid + "&tab=2\">" + username + "</a>"
                            + Constants.getBundleStringByLang(lang, "other.share.notif.title") + sType;
            }
            else {
                String groupIds = StringUtils2.joinIgnoreBlank(",", groups);
                String groupType = "公共圈子";
                RecordSet recs = new RecordSet();

                GroupLogic groupImpl = GlobalLogics.getGroup();
                    groupType = groupImpl.getGroupTypeStr(ctx, lang, groups.get(0));
                    recs = groupImpl.getSimpleGroups(ctx, Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.GROUP_ID_END, groupIds, Constants.GRP_COL_NAME + "," + Constants.GRP_COL_ID);

                ArrayList<String> groupNames = new ArrayList<String>();
                for (Record rec : recs) {
                    String groupName = rec.getString(Constants.GRP_COL_NAME);
                    String groupId = rec.getString(Constants.GRP_COL_ID);
                    groupNames.add("【<a href=\"borqs://profile/details?uid=" + groupId + "&tab=2\">" + groupName + "</a>】");
                }
                if (isEn)
                    returnString = "<a href=\"borqs://profile/details?uid=" + userid + "&tab=2\">" + username + "</a>" + " " + Constants.getBundleStringByLang(lang, "other.share.notif.group.title") + " " + sType
                            + " " + Constants.getBundleStringByLang(lang, "other.share.notif.from") + " " + groupType + StringUtils2.joinIgnoreBlank("，", groupNames);
                else
                    returnString = "<a href=\"borqs://profile/details?uid=" + userid + "&tab=2\">" + username + "</a>" + Constants.getBundleStringByLang(lang, "other.share.notif.from") + groupType + StringUtils2.joinIgnoreBlank("，", groupNames)
                            + Constants.getBundleStringByLang(lang, "other.share.notif.group.title") + sType;
            }
        }
        return returnString;
    }
    
    @Override
    public String getBody(Context ctx, Object... args)
	{
		return (String)args[0];
	}
}