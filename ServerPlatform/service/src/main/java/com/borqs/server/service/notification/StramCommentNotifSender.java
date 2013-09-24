package com.borqs.server.service.notification;

import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StramCommentNotifSender extends NotificationSender {
	private String displayNames = "";
    private String nameLinks = "";

    private static final org.slf4j.Logger L = LoggerFactory.getLogger(StramCommentNotifSender.class);

    public StramCommentNotifSender(Platform p, Qiupu qiupu) {
        super(p, qiupu);
        isReplace = true;
    }
    
    protected List<Long> getScope(String senderId, Object... args) {
        List<Long> userIds = new ArrayList<Long>();
        List<Long> whoComments = new ArrayList<Long>();
        if (getSettingKey().equals(Constants.NTF_MY_STREAM_COMMENT)) {
            try {
                /*
                String s_userid = p.getPost((String) args[0], "source").getString("source");
                userIds.add(Long.parseLong(s_userid));
                RecordSet recs_comments = p.getCommentsFor("",Constants.POST_OBJECT, (String) args[0], "commenter", false, 0, 200);
                for (Record r0 : recs_comments) {
                    Long commenterId = Long.parseLong(r0.getString("commenter"));
                	if (!userIds.contains(commenterId) && !r0.getString("commenter").equals("") && !r0.getString("commenter").equals("0")) {
                        userIds.add(commenterId);                  
                    }
                	if (!whoComments.contains(commenterId) && !r0.getString("commenter").equals("") && !r0.getString("commenter").equals("0")) {
                	    whoComments.add(commenterId);
                	}
                }
                */
                //=========================new send to ,from conversation=========================

                List<String> reasons = new ArrayList<String>();
                reasons.add(String.valueOf(Constants.C_STREAM_POST));
                reasons.add(String.valueOf(Constants.C_STREAM_COMMENT));
                RecordSet conversation_users = p.getConversation(Constants.POST_OBJECT, (String) args[0], reasons, 0, 0, 100);
                for (Record r : conversation_users) {
                    if (!userIds.contains(Long.parseLong(r.getString("from_"))))
                        userIds.add(Long.parseLong(r.getString("from_")));
                }

                List<String> reasons1 = new ArrayList<String>();
                reasons1.add(String.valueOf(Constants.C_STREAM_COMMENT));
                RecordSet conversation_users1 = p.getConversation(Constants.POST_OBJECT, (String) args[0], reasons1, 0, 0, 100);
                for (Record r1 : conversation_users1) {
                    if (!whoComments.contains(Long.parseLong(r1.getString("from_"))))
                        whoComments.add(Long.parseLong(r1.getString("from_")));
                }

                List<String> reasons2 = new ArrayList<String>();
                reasons2.add(String.valueOf(Constants.C_COMMENT_ADDTO));
                RecordSet conversation_users_addto = p.getConversation(Constants.COMMENT_OBJECT, (String) args[2], reasons2, 0, 0, 100);
                for (Record r : conversation_users_addto) {
                    if (!userIds.contains(Long.parseLong(r.getString("from_"))))
                        userIds.add(Long.parseLong(r.getString("from_")));
                }

                //=========================new send to ,from conversation end ====================
            } catch (AvroRemoteException ex) {
                Logger.getLogger(StramCommentNotifSender.class.getName()).log(Level.SEVERE, null, ex);
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
        L.trace("Stream Comment Notification Receive userIds: " + userIds);
        
        return userIds;
    }
    
   @Override
    protected String getSettingKey() {
        return Constants.NTF_MY_STREAM_COMMENT;
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
        String source = (String)args[3];
        String message = (String)args[4];
        String act ="分享";

        if (!receiverId.equals(source))
            act = "评论";
        String returnString = "";
        if (getSettingKey().equals(Constants.NTF_MY_STREAM_COMMENT)) {
            //returnString = "您分享(评论)的<stream id="+streamid+">stream</stream>被<user id="+userid+">"+username+"</user>评论了";
            //您"+act+"的
        	returnString = displayNames + "评论了动态";
            if(StringUtils.isNotBlank(message))
            {
            	returnString += ":" + message;
            }
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
        String source = (String)args[3];
        String message = (String)args[4];
        String commentId = (String)args[5];
        String act ="分享";
        if (!receiverId.equals(source))
            act = "评论";
        String returnString = "";
        if (getSettingKey().equals(Constants.NTF_MY_STREAM_COMMENT)) {
            //returnString = "您分享(评论)的<stream id="+streamid+">stream</stream>被<user id="+userid+">"+username+"</user>评论了";
            //您"+act+"的
        	returnString = nameLinks + "评论了动态";
            if(StringUtils.isNotBlank(message))
            {
            	returnString += ":<a href=\"borqs://stream/comment?id=" +streamid+ "\">"+message+"</a>";
            }
        }
        return returnString;
    }
    
    @Override
    protected String getBody(Object... args)
	{
		return (String)args[0];
	}
}