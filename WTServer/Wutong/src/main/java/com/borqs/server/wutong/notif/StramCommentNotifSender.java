package com.borqs.server.wutong.notif;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.conversation.ConversationLogic;
import com.borqs.server.wutong.ignore.IgnoreLogic;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class StramCommentNotifSender extends NotificationSender {
	private String displayNames = "";
    private String nameLinks = "";
    private ArrayList<Long> toIds = new ArrayList<Long>();

    private static final Logger L = Logger.getLogger(StramCommentNotifSender.class);

    public StramCommentNotifSender() {
        super();
        isReplace = true;
    }
    
    protected List<Long> getScope(Context ctx, String senderId, Object... args) {
        List<Long> userIds = new ArrayList<Long>();
        List<Long> whoComments = new ArrayList<Long>();
        if (getSettingKey(ctx).equals(Constants.NTF_MY_STREAM_COMMENT)) {
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
                reasons.add(String.valueOf(Constants.C_SUBSCRIBE_STREAM));
                reasons.add(String.valueOf(Constants.C_STREAM_COMMENT));
            ConversationLogic conversation = GlobalLogics.getConversation();
            RecordSet conversation_users = conversation.getConversation(ctx, Constants.POST_OBJECT, (String) args[0], reasons, 0, 0, 100);
                for (Record r : conversation_users) {
                    if (!userIds.contains(Long.parseLong(r.getString("from_"))))
                        userIds.add(Long.parseLong(r.getString("from_")));
                }

                List<String> reasons1 = new ArrayList<String>();
                reasons1.add(String.valueOf(Constants.C_STREAM_COMMENT));
                RecordSet conversation_users1 = conversation.getConversation(ctx, Constants.POST_OBJECT, (String) args[0], reasons1, 0, 0, 100);
                for (Record r1 : conversation_users1) {
                    if (!whoComments.contains(Long.parseLong(r1.getString("from_"))))
                        whoComments.add(Long.parseLong(r1.getString("from_")));
                }

                List<String> reasons2 = new ArrayList<String>();
                reasons2.add(String.valueOf(Constants.C_COMMENT_ADDTO));
                RecordSet conversation_users_addto = conversation.getConversation(ctx, Constants.COMMENT_OBJECT, (String) args[2], reasons2, 0, 0, 100);
                for (Record r : conversation_users_addto) {
                    long addToId = Long.parseLong(r.getString("from_"));
                    if (!userIds.contains(addToId)) {
                        userIds.add(addToId);
                    }
                    if (!toIds.contains(addToId)) {
                        toIds.add(addToId);
                    }
                }

                //=========================new send to ,from conversation end ====================
        }
        if (userIds.contains(Long.parseLong((String)args[1])))
            userIds.remove(Long.parseLong((String)args[1]));
        
        //exclude sender
        if(StringUtils.isNotBlank(senderId))
        {
        	userIds.remove(Long.parseLong(senderId));
        }
        try {
            IgnoreLogic ignore = GlobalLogics.getIgnore();
            userIds = ignore.formatIgnoreUserListP(ctx, userIds, (String) args[0], "");
            whoComments = ignore.formatIgnoreUserListP(ctx, whoComments, (String) args[0], "");
        } catch (Exception e) {
        }
			List<String> l = new ArrayList<String>();
			int size = whoComments.size();
			if (size > 4) {
				size = 4;
			}
			for (int i = 0; i < size; i++) {
				String userId = String.valueOf(whoComments.get(i));
                AccountLogic account = GlobalLogics.getAccount();
                String displayName = account.getUser(ctx, userId, userId, "display_name")
						.getString("display_name", "");
				
				displayNames += displayName + ", ";
				l.add("<a href=\"borqs://profile/details?uid=" + userId + "&tab=2\">" + displayName + "</a>");				
			}
			
			nameLinks = StringUtils.join(l, ", ");
			if(StringUtils.isNotBlank(displayNames))
			{
				displayNames = StringUtils.substringBeforeLast(displayNames, ",");				
			}
        L.trace(ctx, "Stream Comment Notification Receive userIds: " + userIds);
        
        return userIds;
    }
    
   @Override
    protected String getSettingKey(Context ctx) {
        return Constants.NTF_MY_STREAM_COMMENT;
    }

    @Override
    protected String getAppId(Context ctx, Object... args) {
        return String.valueOf(Constants.APP_TYPE_BPC);
    }

    @Override
    protected String getTitle(Context ctx, String lang, Object... args) {
        String streamid = (String)args[0];
        String userid = (String)args[1];
        String username = (String)args[2];
        String source = (String)args[3];
        String message = (String)args[4];
        String act ="分享";

        if (!receiverId.equals(source))
            act = "评论";
        String returnString = "";
        if (getSettingKey(ctx).equals(Constants.NTF_MY_STREAM_COMMENT)) {
            //returnString = "您分享(评论)的<stream id="+streamid+">stream</stream>被<user id="+userid+">"+username+"</user>评论了";
            //您"+act+"的
        	returnString = displayNames + Constants.getBundleStringByLang(lang, "stream.comment.notif");
            if(StringUtils.isNotBlank(message))
            {
            	returnString += ":" + message;
            }
        }
        return returnString;
    }
    
    @Override
    protected String getUri(Context ctx, Object... args) {
        return "borqs://stream/comment?id=" + (String)args[0] + "&comment_id=" + (String)args[1];
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
        if (getSettingKey(ctx).equals(Constants.NTF_MY_STREAM_COMMENT)) {
            //returnString = "您分享(评论)的<stream id="+streamid+">stream</stream>被<user id="+userid+">"+username+"</user>评论了";
            //您"+act+"的
        	returnString = nameLinks + Constants.getBundleStringByLang(lang, "stream.comment.notif");
            if(StringUtils.isNotBlank(message))
            {
            	returnString += ":<a href=\"borqs://stream/comment?id=" +streamid+ "\">"+message+"</a>";
            }
        }
        return returnString;
    }
    
    @Override
    protected String getBody(Context ctx, Object... args)
	{
		return (String)args[0];
	}
}