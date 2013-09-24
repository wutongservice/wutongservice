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

public class PhotoSharedNotifSender extends GroupNotificationSender {
    private ArrayList<Long> groups = new ArrayList<Long>();
    private HashSet<Long> toIds = new HashSet<Long>();

    public PhotoSharedNotifSender() {
        super();
        isReplace = true;
    }
    
    @Override
	public List<Long> getScope(Context ctx, String senderId, RecordSet recs, Object... args) {
		List<Long> userIds = new ArrayList<Long>();

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
        return Constants.NTF_PHOTO_SHARE;
    }

    @Override
    protected String getAppId(Context ctx, Object... args) {
        String sType = (String)args[0];
//    	return String.valueOf(findAppIdFromPostType((Integer)args[0]));
        return String.valueOf(findAppIdFromPostType(ctx, Integer.parseInt(sType)));
    }

    
    
    @Override
    public String getTitle(Context ctx, String lang, Object... args) {
        boolean isEn = StringUtils.contains(lang, "en") ? true : false;
        String sType = Constants.getBundleStringByLang(lang, "file.share.notif.photo");

        if (groups.isEmpty()) {
            if (isEn)
                return args[1] + " " + Constants.getBundleStringByLang(lang, "other.share.notif.title") + " " + sType;
            else
                return args[1] + Constants.getBundleStringByLang(lang, "other.share.notif.title") + sType;
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
                return args[1] + " " + Constants.getBundleStringByLang(lang, "other.share.notif.group.title") + " " + sType
                        + " " +Constants.getBundleStringByLang(lang, "other.share.notif.from") + " " + groupType + StringUtils2.joinIgnoreBlank("，", groupNames);
            else
                return args[1] + Constants.getBundleStringByLang(lang, "other.share.notif.from") + groupType + StringUtils2.joinIgnoreBlank("，", groupNames)
                        + Constants.getBundleStringByLang(lang, "other.share.notif.group.title") + sType;
        }
    }

    @Override
    protected String getUri(Context ctx, Object... args) {
        Object photo_id = args[0];
        return "borqs://photo/comment?id=" + photo_id;
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
        boolean isEn = StringUtils.contains(lang, "en") ? true : false;
        String sType = Constants.getBundleStringByLang(lang, "file.share.notif.photo");

        if (groups.isEmpty()) {
            if (isEn)
                return "<a href=\"borqs://profile/details?uid=" + args[1] + "&tab=2\">" + args[2]+ "</a>"
                        + " " + Constants.getBundleStringByLang(lang, "other.share.notif.title") + " " + sType;
            else
                return "<a href=\"borqs://profile/details?uid=" + args[1] + "&tab=2\">" + args[2]+ "</a>"
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
                return "<a href=\"borqs://profile/details?uid=" + args[1] + "&tab=2\">" + args[2]+ "</a>" + " " + Constants.getBundleStringByLang(lang, "other.share.notif.group.title") + " " + sType
                        + " " + Constants.getBundleStringByLang(lang, "other.share.notif.from") + " " + groupType + StringUtils2.joinIgnoreBlank("，", groupNames);
            else
                return "<a href=\"borqs://profile/details?uid=" + args[1] + "&tab=2\">" + args[2]+ "</a>" + Constants.getBundleStringByLang(lang, "other.share.notif.from") + groupType + StringUtils2.joinIgnoreBlank("，", groupNames)
                        + Constants.getBundleStringByLang(lang, "other.share.notif.group.title") + sType;
        }
    }
    
    @Override
    public String getBody(Context ctx, Object... args)
	{
		return (String)args[0];
	}
}