package com.borqs.server.wutong.notif;

import com.borqs.server.base.ResponseError;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.account2.AccountImpl;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.friendship.FriendshipImpl;
import com.borqs.server.wutong.friendship.FriendshipLogic;
import com.borqs.server.wutong.ignore.IgnoreImpl;
import com.borqs.server.wutong.ignore.IgnoreLogic;
import com.borqs.server.wutong.setting.SettingImpl;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class NewFollowerNotifSender extends NotificationSender {
    private static final Logger L = Logger.getLogger(NewFollowerNotifSender.class);

    private String displayNames = "";
    private String nameLinks = "";
	
	public NewFollowerNotifSender() {
		super();
		isReplace = true;
	}

	@Override
	protected List<Long> getScope(Context ctx, String senderId, Object... args) {
		List<Long> scope = new ArrayList<Long>();
		
		for(int i = 0; i < args.length; i++)
		{
			scope.add(Long.parseLong((String)args[i]));
		}
		
		//exclude sender
		if(StringUtils.isNotBlank(senderId))
  		{
  			scope.remove(Long.parseLong(senderId));
  		}

        try {
            IgnoreLogic ignore = GlobalLogics.getIgnore();
            scope = ignore.formatIgnoreUserListP(ctx, scope, "", "");
        } catch (Exception e) {
        }
		return scope;
	}

	@Override
	protected String getSettingKey(Context ctx) {
		return Constants.NTF_NEW_FOLLOWER;
	}
	
	@Override
	protected String getAppId(Context ctx, Object... args)
	{
		return String.valueOf(Constants.APP_TYPE_BPC);
	}
		
	@Override
	protected String getTitle(Context ctx, String lang, Object... args)
	{
		String senderId = (String)args[0];		
		List<String> l = new ArrayList<String>();
		nameLinks = "";

        AccountLogic account = GlobalLogics.getAccount();
        String displayName = account.getUser(ctx, senderId, senderId, "display_name").getString("display_name", "");
			displayNames = displayName + ", ";
			l.add("<a href=\"borqs://profile/details?uid=" + senderId + "&tab=2\">" + displayName + "</a>");

        FriendshipLogic friendship = GlobalLogics.getFriendship();
        RecordSet rs = friendship.getFollowersP(ctx, receiverId, receiverId,
                String.valueOf(Constants.FRIENDS_CIRCLE), "user_id,display_name", 0, 1000);
						
			int i = 0;
            for(Record user : rs)
            {
            	if(i > 4)
            		break;
            	           	
            	long createdTime = user.getInt("relation_created_time");
            	long period = 1 * 24 * 60 * 60 * 1000;
            	if (createdTime < (new Date().getTime() - period)) {
                    continue;
                }
            	
            	String userId = user.getString("user_id");
            	displayName = user.getString("display_name");
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

		return "您有新的粉丝" + displayNames;
	}
	
	@Override
	protected String getUri(Context ctx, Object... args)
	{
		return "borqs://userlist/fans?uid=" + receiverId;
	}
	
	@Override
	protected String getTitleHtml(Context ctx, String lang, Object... args)
	{
//		String userId = (String)args[0];
//		String displayName = "";
//		
//		try {
//			displayName = p.getUser(userId, userId, "display_name").getString("display_name", "");
//		} catch (AvroRemoteException e) {			
//			Logger.getLogger(StreamLikeNotifSender.class.getName()).log(Level.SEVERE, null, e);
//		}
		
//		return "您有新的粉丝<a href=\"borqs://profile/details?uid=" + args[0] + "&tab=2\">" + displayName + "</a>";
		return "您有新的粉丝" + nameLinks;
	}
	
	@Override
	public void send(Context ctx, Object[] scopeArgs, Object[][] args) {
        final String METHOD = "send";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, scopeArgs, args);

        List<Long> l = getScope(ctx, getSenderId(ctx, args[1]), scopeArgs);
        L.trace(ctx, "send notification scope: " + l);
		if(l.size() > 0)
		{
            SettingImpl settingImpl = new SettingImpl();
            settingImpl.init();
            Record setting = settingImpl.getByUsers(ctx, getSettingKey(ctx), StringUtils.join(l, ","));
		
			Iterator iter = setting.entrySet().iterator();
			
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				receiverId = (String) entry.getKey();
                L.debug(ctx, "send notification receiverId: " + receiverId);
				String value = (String) entry.getValue();
                
				if(value.equals("0"))
				{
                    String language = GlobalLogics.getAccount().getUser(ctx, ctx.getViewerIdString(),
                            receiverId, "user_id,language").getString("language", "en");
                    if (StringUtils.contains(language, "zh"))
                        zhRecvId = receiverId;
                    else
                        enRecvId = receiverId;
                    Record msg = createNotification(ctx, language, args);
		            try{
                        L.debug(ctx, "send notification content: " + msg.toString(true, true));
                        String result = notif.send(msg, isReplace);
                        L.debug(ctx, "send notification result: " + result);
		            }
		            catch(Exception e){
		                
		            }
				}				
			}			     
		}

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
    }
}