package com.borqs.server.service.notification;

import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.qiupu.Qiupu;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;


public class PeopleYouMayKnowNotifSender extends NotificationSender {
	private String displayNames = "";
    private String nameLinks = "";	
	
	public PeopleYouMayKnowNotifSender(Platform p, Qiupu qiupu) {
		super(p, qiupu);		
	}

	 @Override
    protected List<Long> getScope(String senderId, Object... args) {
        List<Long> userIds = new ArrayList<Long>();
        userIds.add(Long.parseLong((String) args[0]));
        
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
        return Constants.NTF_PEOPLE_YOU_MAY_KNOW;
    }

    @Override
    protected String getAppId(Object... args) {
        return String.valueOf(Constants.APP_TYPE_BPC);
    }

    @Override
    protected String getTitle(Object... args) {
        return "您有新的好友可能认识，快去看看吧！";
    }

    @Override
    protected String getUri(Object... args) {
    	return "borqs://friends/details?uid=" + receiverId + "&tab=2";
    }

    @Override
    protected String getTitleHtml(Object... args) {
    	return "您有新的好友可能认识，快去看看吧！";
    }

    @Override
    protected String getBody(Object... args)
	{
		String viewerId = (String)args[0];
    	String ulc =  (String)args[1];
		List<String> l = StringUtils2.splitList(ulc, ",", true);
		try
		{
			RecordSet rs = p.getUsers(viewerId, ulc, "user_id,display_name");
			
			int i = 0;
			for(Record user : rs)
			{
				if(i > 4)
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
		}
		catch (AvroRemoteException e) {			
			Logger.getLogger(NewFollowerNotifSender.class.getName()).log(Level.SEVERE, null, e);
		}
		
		return displayNames;
	}
    
    @Override
    protected String getBodyHtml(Object... args)
    {
    	return nameLinks;
    }
}