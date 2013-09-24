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

public class PhotoSharedNotifSender extends GroupNotificationSender {
    private ArrayList<Long> groups = new ArrayList<Long>();

    public PhotoSharedNotifSender(Platform p, Qiupu qiupu) {
        super(p, qiupu);
        isReplace = true;
    }
    
    @Override
	public List<Long> getScope(String senderId, Object... args) {
		List<Long> userIds = new ArrayList<Long>();

		try {
            List<String> reasons = new ArrayList<String>();
            reasons.add(String.valueOf(Constants.C_STREAM_TO));
            reasons.add(String.valueOf(Constants.C_STREAM_ADDTO));
            RecordSet conversation_users = p.getConversation(Constants.POST_OBJECT, (String) args[0], reasons, 0, 0, 100);
            for (Record r : conversation_users) {
                long userId = Long.parseLong(r.getString("from_"));
                if (!userIds.contains(userId)) {
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
            }
            //=========================new send to ,from conversation end ====================
		} catch (AvroRemoteException ex) {
			Logger.getLogger(SharedAppNotifSender.class.getName()).log(
					Level.SEVERE, null, ex);
		}

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
        return Constants.NTF_PHOTO_SHARE;
    }

    @Override
    protected String getAppId(Object... args) {
        String sType = (String)args[0];
//    	return String.valueOf(findAppIdFromPostType((Integer)args[0]));
        return String.valueOf(findAppIdFromPostType(Integer.parseInt(sType)));
    }

    
    
    @Override
    public String getTitle(Object... args) {
        if (groups.isEmpty())
            return args[1] + "给您分享了他的照片";
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
            return args[1] + "在" + groupType + StringUtils2.joinIgnoreBlank("，", groupNames) + "分享了他的照片";
        }
    }

    @Override
    protected String getUri(Object... args) {
        Object photo_id = args[0];
        return "borqs://photo/comment?id=" + photo_id;
    }
    
    @Override
    protected String getTitleHtml(Object... args) {
        if (groups.isEmpty())
            return "<a href=\"borqs://profile/details?uid=" + args[1] + "&tab=2\">" + args[2]+ "</a>给您分享了他的照片";
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
            return "<a href=\"borqs://profile/details?uid=" + args[1] + "&tab=2\">" + args[2] + "</a>在" + groupType + StringUtils2.joinIgnoreBlank("，", groupNames) + "分享了他的照片";
        }
    }
    
    @Override
    public String getBody(Object... args)
	{
		return (String)args[0];
	}
}