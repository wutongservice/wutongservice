package com.borqs.server.service.notification;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;

import com.borqs.server.ErrorCode;
import com.borqs.server.base.ResponseError;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Errors;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.qiupu.Qiupu;


public class NewFollowerNotifSender extends NotificationSender {
    private String displayNames = "";
    private String nameLinks = "";
	
	public NewFollowerNotifSender(Platform p, Qiupu qiupu) {
		super(p, qiupu);
		isReplace = true;
	}

	@Override
	protected List<Long> getScope(String senderId, Object... args) {		
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
           scope = p.formatIgnoreUserList(senderId, scope, "","");
        } catch (Exception e) {
        }
		return scope;
	}

	@Override
	protected String getSettingKey() {
		return Constants.NTF_NEW_FOLLOWER;
	}
	
	@Override
	protected String getAppId(Object... args)
	{
		return String.valueOf(Constants.APP_TYPE_BPC);
	}
		
	@Override
	protected String getTitle(Object... args)
	{
		String senderId = (String)args[0];		
		List<String> l = new ArrayList<String>();
		nameLinks = "";
		
		try {
			String displayName = p.getUser(senderId, senderId, "display_name").getString("display_name", "");
			displayNames = displayName + ", ";
			l.add("<a href=\"borqs://profile/details?uid=" + senderId + "&tab=2\">" + displayName + "</a>");
			
			RecordSet rs = p.getFollowers(receiverId, receiverId, 
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
		} catch (AvroRemoteException e) {			
			Logger.getLogger(NewFollowerNotifSender.class.getName()).log(Level.SEVERE, null, e);
		}
		
		return "您有新的粉丝" + displayNames;
	}
	
	@Override
	protected String getUri(Object... args)
	{
		return "borqs://userlist/fans?uid=" + receiverId;
	}
	
	@Override
	protected String getTitleHtml(Object... args)
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
	public void send(Object[] scopeArgs, Object[][] args) throws ResponseError, AvroRemoteException {
		List<Long> l = getScope(getSenderId(args[1]), scopeArgs);
		if(l.size() > 0)
		{
			Record setting = p.getPreferencesByUsers(getSettingKey(), StringUtils.join(l, ","));
		
			Iterator iter = setting.entrySet().iterator();
			
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				receiverId = (String) entry.getKey();
				String value = (String) entry.getValue();
                
				if(value.equals("0"))
				{			
					Record msg = createNotification(args); 
		            try{
		            	notif.send(msg, isReplace);
		            }
		            catch(Exception e){
		                
		            }
				}				
			}			     
		}
	}
}