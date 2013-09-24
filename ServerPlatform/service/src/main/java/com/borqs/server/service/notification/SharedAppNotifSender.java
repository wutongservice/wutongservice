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
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SharedAppNotifSender extends GroupNotificationSender {

    private ArrayList<Long> groups = new ArrayList<Long>();

    public SharedAppNotifSender(Platform p, Qiupu qiupu) {
        super(p, qiupu);
    }
    
    public List<Long> getScope(String senderId, Object... args) {
        List<Long> userIds = new ArrayList<Long>();
        if (getSettingKey().equals(Constants.NTF_APP_SHARE)) {
            try {
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
                RecordSet conversation_users = p.getConversation(Constants.POST_OBJECT, (String) args[0], reasons, 0, 0, 100);
                for (Record r : conversation_users) {
                    long userId = Long.parseLong(r.getString("from_"));
                    if (!userIds.contains(userId))
                        if ((userId >= Constants.PUBLIC_CIRCLE_ID_BEGIN)
                                && (userId <= Constants.GROUP_ID_END)) {
                            groups.add(userId);
                            String members = p.getGroupMembers(userId);
                            List<Long> memberIds = StringUtils2.splitIntList(members, ",");
                            userIds.addAll(memberIds);
                        }
                        else
                            userIds.add(userId);
                }
                //=========================new send to ,from conversation end ====================
            } catch (AvroRemoteException ex) {
                Logger.getLogger(SharedAppNotifSender.class.getName()).log(Level.SEVERE, null, ex);
            }
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
           userIds = p.formatIgnoreUserList(senderId, userIds, "","");
        } catch (Exception e) {
        }
        return userIds;
    }
    
   @Override
    protected String getSettingKey() {
        return Constants.NTF_APP_SHARE;
    }

    @Override
    protected String getAppId(Object... args) {
//        return String.valueOf(Constants.APP_TYPE_QIUPU);
        return (String) args[0];
    }

    @Override
    public String getTitle(Object... args) {
        String apk_id = (String)args[0];
        String userid = (String)args[1];
        String username = (String)args[2];
        String appname = (String)args[3];
        String returnString = "";
        if (getSettingKey().equals(Constants.NTF_APP_SHARE)) {
            //returnString = "您的朋友<user id="+userid+">"+username+"</user>给您分享了应用 <apk id="+apk_id+">"+appname+"</apk>";
            if (groups.isEmpty())
                returnString = username + "给您分享了应用 " + appname;
            else {
                String groupIds = StringUtils2.joinIgnoreBlank(",", groups);
                String groupType = "公共圈子";
                RecordSet recs = new RecordSet();

                try {
                    groupType = p.getGroupTypeStr(groups.get(0), "");
                    recs = p.getGroups(Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.GROUP_ID_END, groupIds, Constants.GRP_COL_NAME);
                } catch (AvroRemoteException ex) {
                    Logger.getLogger(SharedAppNotifSender.class.getName()).log(
                            Level.SEVERE, null, ex);
                }

                ArrayList<String> groupNames = new ArrayList<String>();
                for (Record rec : recs) {
                    String groupName = rec.getString(Constants.GRP_COL_NAME);
                    groupNames.add("【" + groupName + "】");
                }
                returnString = username + "在" + groupType + StringUtils2.joinIgnoreBlank("，", groupNames) + "分享了应用 " + appname;
            }
        }
        return returnString;
    }

    @Override
    protected String getAction(Object... args) {
        return "android.intent.action.VIEW";
    }

    @Override
    protected String getUri(Object... args) {
        String apk_id = (String)args[0];
        return "borqs://application/details?id="+apk_id+"";
    }
    
    @Override
    protected String getTitleHtml(Object... args) {
        String apk_id = (String)args[0];
        String userid = (String)args[1];
        String username = (String)args[2];
        String appname = (String)args[3];
        String returnString = "";
        if (getSettingKey().equals(Constants.NTF_APP_SHARE)) {
            //returnString = "您的朋友<user id="+userid+">"+username+"</user>给您分享了应用 <apk id="+apk_id+">"+appname+"</apk>";
            if (groups.isEmpty())
                returnString = "<a href=\"borqs://profile/details?uid=" + userid + "&tab=2\">"+username+"</a>给您分享了应用:<a href=\"borqs://application/details?id=" + apk_id + "\">"+appname+"</a>";
            else {
                String groupIds = StringUtils2.joinIgnoreBlank(",", groups);
                String groupType = "公共圈子";
                RecordSet recs = new RecordSet();

                try {
                    groupType = p.getGroupTypeStr(groups.get(0), "");
                    recs = p.getGroups(Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.GROUP_ID_END, groupIds, Constants.GRP_COL_NAME + "," + Constants.GRP_COL_ID);
                } catch (AvroRemoteException ex) {
                    Logger.getLogger(SharedAppNotifSender.class.getName()).log(
                            Level.SEVERE, null, ex);
                }

                ArrayList<String> groupNames = new ArrayList<String>();
                for (Record rec : recs) {
                    String groupName = rec.getString(Constants.GRP_COL_NAME);
                    String groupId = rec.getString(Constants.GRP_COL_ID);
                    groupNames.add("【<a href=\"borqs://profile/details?uid=" + groupId + "&tab=2\">" + groupName + "</a>】");
                }
                returnString = "<a href=\"borqs://profile/details?uid=" + userid + "&tab=2\">" + username+ "</a>在" + groupType + StringUtils2.joinIgnoreBlank("，", groupNames) + "分享了应用:<a href=\"borqs://application/details?id=" + apk_id + "\">"+appname+"</a>";
            }
        }
        return returnString;
    }
    
    @Override
    public String getBody(Object... args)
	{
		return (String)args[0];
	}
}