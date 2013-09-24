package com.borqs.server.wutong.notif;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.account2.AccountImpl;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.conversation.ConversationImpl;
import com.borqs.server.wutong.conversation.ConversationLogic;
import com.borqs.server.wutong.ignore.IgnoreImpl;
import com.borqs.server.wutong.ignore.IgnoreLogic;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StreamLikeNotifSender extends NotificationSender {
	private String displayNames = "";
    private String nameLinks = "";
	
    public StreamLikeNotifSender() {
        super();
        isReplace = true;
    }

    @Override
    protected List<Long> getScope(Context ctx, String senderId, Object... args) {
        List<Long> userIds = new ArrayList<Long>();
        RecordSet liked_Users = new RecordSet();
        if (getSettingKey(ctx).equals(Constants.NTF_MY_STREAM_LIKE)) {
                    /*
                    String s_userid = p.getPost((String)args[0], "source").getString("source");
                    userIds.add(Long.parseLong(s_userid));
                
                    liked_Users = p.likedUsers(Constants.POST_OBJECT, (String)args[0], 0, 200);
                    */
                    //=========================new send to ,from conversation=========================

                    List<String> reasons = new ArrayList<String>();
                    reasons.add(String.valueOf(Constants.C_STREAM_POST));
                    reasons.add(String.valueOf(Constants.C_SUBSCRIBE_STREAM));
            ConversationLogic conversation = GlobalLogics.getConversation();
            RecordSet conversation_users = conversation.getConversation(ctx, Constants.POST_OBJECT, (String) args[0], reasons, 0, 0, 100);
                    for (Record r : conversation_users) {
                        if (!userIds.contains(Long.parseLong(r.getString("from_"))))
                            userIds.add(Long.parseLong(r.getString("from_")));
                    }

                    List<String> reasons1 = new ArrayList<String>();
                    reasons1.add(String.valueOf(Constants.C_STREAM_LIKE));
                    RecordSet conversation_users1 = conversation.getConversation(ctx, Constants.POST_OBJECT, (String) args[0], reasons1, 0, 0, 100);
            AccountLogic account = GlobalLogics.getAccount();
            liked_Users.addAll(account.getUsers(ctx, (String) args[1], conversation_users1.joinColumnValues("from_", ","), "user_id,display_name", false));
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
            userIds = ignore.formatIgnoreUserListP(ctx, userIds, "", (String) args[0]);
            List<Long> lu = new ArrayList<Long>();
            for (Record r : liked_Users) {
                lu.add(Long.parseLong(r.getString("user_id")));
            }
            lu = ignore.formatIgnoreUserListP(ctx, lu, "", (String) args[0]);
            for (int i = liked_Users.size() - 1; i >= 0; i--) {
                if (!lu.contains(Long.parseLong(liked_Users.get(i).getString("user_id"))))
                    liked_Users.remove(i);
            }
        } catch (Exception e) {
        }

        List<String> l = new ArrayList<String>();
        int i = 0;
        for(Record user: liked_Users)
        {
        	if(i > 3)
        		break;
        	
        	String userId = user.getString("user_id");
        	String displayName = user.getString("display_name");
        	if(StringUtils.contains(displayNames, displayName))
        	{
        		continue;
        	}
        	else
        	{
        		displayNames += displayName + ", ";
				l.add("<a href=\"borqs://profile/details?uid=" + userId + "&tab=2\">" + displayName + "</a>");
				i++;
        	}
        }
        
        nameLinks = StringUtils.join(l, ", ");
		if(StringUtils.isNotBlank(displayNames))
		{
			displayNames = StringUtils.substringBeforeLast(displayNames, ",");				
		}
		
        return userIds;
    }

    @Override
    protected String getSettingKey(Context ctx) {
        return Constants.NTF_MY_STREAM_LIKE;
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
        String returnString = "";
        if (getSettingKey(ctx).equals(Constants.NTF_MY_STREAM_LIKE)) {
            //returnString = "您分享(评论)的<stream id="+streamid+">stream</stream>被<user id="+userid+">"+username+"</user> like了";
            //您"+act+"的
        	returnString = displayNames + "赞了动态";
            if(StringUtils.isNotBlank(message))
            {
            	returnString += ":" + message;
            }
        }
        return returnString;
    }

    @Override
    protected String getUri(Context ctx, Object... args) {
        return "borqs://stream/comment?id="+(String)args[0];
    }
    
    @Override
    protected String getTitleHtml(Context ctx, String lang, Object... args) {
        String streamid = (String)args[0];
        String userid = (String)args[1];
        String username = (String)args[2];
        String source = (String)args[3];
        String message = (String)args[4];
        String act ="分享";
        if (!userid.equals(source))
            act = "评论";
        String returnString = "";
        if (getSettingKey(ctx).equals(Constants.NTF_MY_STREAM_LIKE)) {
            //returnString = "您分享(评论)的<stream id="+streamid+">stream</stream>被<user id="+userid+">"+username+"</user> like了";
            //您"+act+"的
        	returnString = nameLinks + "赞了动态";
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