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

public class StreamRetweetNotifSender extends NotificationSender {
	private String displayNames = "";
    private String nameLinks = "";

    public StreamRetweetNotifSender(Platform p, Qiupu qiupu) {
        super(p, qiupu);
        isReplace = true;
    }

    @Override
    protected List<Long> getScope(String senderId, Object... args) {
        List<Long> userIds = new ArrayList<Long>();
        RecordSet retweeted_Users = new RecordSet();
        if (getSettingKey().equals(Constants.NTF_MY_STREAM_RETWEET)) {
            try {
                /*
                String s_userid = p.getPost((String) args[0], "source").getString("source");
                userIds.add(Long.parseLong(s_userid));
                
                retweeted_Users = p.findWhoRetweetStream((String)args[0], 200);
                */
                //=========================new send to ,from conversation=========================

                List<String> reasons = new ArrayList<String>();
                reasons.add(String.valueOf(Constants.C_STREAM_POST));
                RecordSet conversation_users = p.getConversation(Constants.POST_OBJECT, (String) args[0], reasons, 0, 0, 100);
                userIds.add(Long.parseLong(conversation_users.getFirstRecord().getString("from_")));

                List<String> reasons1 = new ArrayList<String>();
                reasons1.add(String.valueOf(Constants.C_STREAM_RESHARE));
                RecordSet conversation_users1 = p.getConversation(Constants.POST_OBJECT, (String) args[0], reasons1, 0, 0, 100);
                retweeted_Users =   conversation_users1;

//                List<String> reasons2 = new ArrayList<String>();
//                reasons2.add(String.valueOf(Constants.C_STREAM_ADDTO));
//                reasons2.add(String.valueOf(Constants.C_STREAM_TO));
//                RecordSet conversation_users2 = p.getConversation(Constants.POST_OBJECT, (String) args[2], reasons2, 0, 0, 100);
//                for (Record r : conversation_users2) {
//                    if (!userIds.contains(Long.parseLong(r.getString("from_"))))
//                        userIds.add(Long.parseLong(r.getString("from_")));
//                }

//                retweeted_Users.addAll(p.getUsers((String) args[1], conversation_users1.joinColumnValues("from_", ","), "user_id,display_name", false));
                //=========================new send to ,from conversation end ====================

                List<String> l = new ArrayList<String>();
                int i = 0;
                for(Record user: retweeted_Users)
                {
                	if(i > 3)
                		break;
                	
                	String userId = user.getString("from_");
                	String displayName = p.getUser(userId, userId, "display_name")
        					.getString("display_name", "");
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
            } catch (AvroRemoteException ex) {
                Logger.getLogger(StreamRetweetNotifSender.class.getName()).log(Level.SEVERE, null, ex);
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
           userIds = p.formatIgnoreUserList(senderId, userIds, "","");
        } catch (Exception e) {
        }
        return userIds;
    }

    @Override
    protected String getSettingKey() {
        return Constants.NTF_MY_STREAM_RETWEET;
    }

    @Override
    protected String getAppId(Object... args) {
        return String.valueOf(Constants.APP_TYPE_BPC);
    }

    @Override
    protected String getTitle(Object... args) {
        String streamid = (String) args[0];
        String userid = (String) args[1];
        String username = (String) args[2];
        String oldstreamid = (String) args[3];
        String oldstreamsource = (String) args[4];
        String oldstreammessage = (String) args[5];
        String returnString = "";
        if (getSettingKey().equals(Constants.NTF_MY_STREAM_RETWEET)) {
            //returnString = "您分享(评论)的<stream id=" + streamid + ">stream</stream>被<user id=" + userid + ">" + username + "</user> 转推了";
            //returnString = "{0}转推了您分享的动态";
            returnString = displayNames + "引用了来源于你的分享:"+oldstreammessage;
        }
        return returnString;
    }
    
    @Override
    protected String getUri(Object... args) {
        return "borqs://stream/comment?id="+(String)args[0];
    }
    @Override
    protected String getTitleHtml(Object... args) {
        String streamid = (String) args[0];
        String userid = (String) args[1];
        String username = (String) args[2];
        String oldstreamid = (String) args[3];
        String oldstreamsource = (String) args[4];
        String oldstreammessage = (String) args[5];
        String returnString = "";
        if (getSettingKey().equals(Constants.NTF_MY_STREAM_RETWEET)) {
            //returnString = "您分享(评论)的<stream id=" + streamid + ">stream</stream>被<user id=" + userid + ">" + username + "</user> 转推了";
            //returnString = "{0}转推了您分享的动态";
            returnString = nameLinks + " <a href=\"borqs://stream/comment?id=" + streamid + "\">引用</a> 了来源于你的分享:<a href=\"borqs://stream/comment?id=" + oldstreamid + "\">"+oldstreammessage+"</a>";
        }
        return returnString;
    }
    
    @Override
    protected String getBody(Object... args)
	{
		return (String)args[0];
	}
    
}