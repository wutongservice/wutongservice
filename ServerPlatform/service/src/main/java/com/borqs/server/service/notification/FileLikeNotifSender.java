package com.borqs.server.service.notification;

import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileLikeNotifSender extends NotificationSender {
	private String displayNames = "";
    private String nameLinks = "";

    public FileLikeNotifSender(Platform p, Qiupu qiupu) {
        super(p, qiupu);
        isReplace = true;
    }
    
    protected List<Long> getScope(String senderId, Object... args) {
        List<Long> userIds = new ArrayList<Long>();
        List<Long> whoComments = new ArrayList<Long>();
        if (getSettingKey().equals(Constants.NTF_FILE_LIKE)) {
            try {
                List<String> reasons = new ArrayList<String>();
                reasons.add(String.valueOf(Constants.C_FILE_SHARE));
                reasons.add(String.valueOf(Constants.C_FILE_LIKE));
                RecordSet conversation_users = p.getConversation(Constants.FILE_OBJECT, (String) args[0], reasons, 0, 0, 100);
                for (Record r : conversation_users) {
                    if (!userIds.contains(Long.parseLong(r.getString("from_"))))
                        userIds.add(Long.parseLong(r.getString("from_")));
                }

                List<String> reasons1 = new ArrayList<String>();
                reasons1.add(String.valueOf(Constants.C_FILE_LIKE));
                RecordSet conversation_users1 = p.getConversation(Constants.FILE_OBJECT, (String) args[0], reasons1, 0, 0, 100);
                for (Record r1 : conversation_users1) {
                    if (!whoComments.contains(Long.parseLong(r1.getString("from_"))))
                        whoComments.add(Long.parseLong(r1.getString("from_")));
                }
                //=========================new send to ,from conversation end ====================
            } catch (AvroRemoteException ex) {
                Logger.getLogger(FileLikeNotifSender.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (userIds.contains(Long.parseLong((String)args[1])))
            userIds.remove(Long.parseLong((String)args[1]));
        
        //exclude sender
        if(StringUtils.isNotBlank(senderId))
        {
        	userIds.remove(Long.parseLong(senderId));
        }
        try {
            userIds = p.formatIgnoreUserList(senderId, userIds, (String) args[0], "");
            whoComments = p.formatIgnoreUserList(senderId, whoComments, (String) args[0], "");
        } catch (Exception e) {
        }
        try {
			List<String> l = new ArrayList<String>();
			int size = whoComments.size();
			if (size > 4) {
				size = 4;
			}
			for (int i = 0; i < size; i++) {
				String userId = String.valueOf(whoComments.get(i));
				String displayName = p.getUser(userId, userId, "display_name")
						.getString("display_name", "");
				
				displayNames += displayName + ", ";
				l.add("<a href=\"borqs://profile/details?uid=" + userId + "&tab=2\">" + displayName + "</a>");				
			}
			
			nameLinks = StringUtils.join(l, ", ");
			if(StringUtils.isNotBlank(displayNames))
			{
				displayNames = StringUtils.substringBeforeLast(displayNames, ",");				
			}
		} catch (AvroRemoteException e) {

		}
        
        return userIds;
    }
    
   @Override
    protected String getSettingKey() {
        return Constants.NTF_FILE_LIKE;
    }

    @Override
    protected String getAppId(Object... args) {
        return String.valueOf(Constants.APP_TYPE_BPC);
    }

    @Override
    protected String getTitle(Object... args) {
        String streamid = (String)args[0];
        String userid = (String)args[1];
        String username = (String)args[2];
        String act ="分享";

        if (!receiverId.equals(userid))
            act = "评论";
        String returnString = "";
        if (getSettingKey().equals(Constants.NTF_FILE_LIKE)) {
        	returnString = displayNames + "赞了文件";
        }
        return returnString;
    }
    
    @Override
    protected String getUri(Object... args) {
        return "borqs://stream/comment?id="+(String)args[0];
    }
    
    @Override
    protected String getTitleHtml(Object... args) {
        String streamid = (String)args[0];
        String userid = (String)args[1];
        String username = (String)args[2];
        String act ="分享";
        if (!receiverId.equals(userid))
            act = "评论";
        String returnString = "";
        if (getSettingKey().equals(Constants.NTF_FILE_LIKE)) {
        	returnString = nameLinks + "赞了文件";
        }
        return returnString;
    }
    
//    @Override
//    protected String getBody(Object... args)
//	{
//		return (String)args[0];
//	}
}