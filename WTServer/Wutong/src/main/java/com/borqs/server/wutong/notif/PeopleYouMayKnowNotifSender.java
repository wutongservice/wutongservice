package com.borqs.server.wutong.notif;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.account2.AccountImpl;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.ignore.IgnoreImpl;
import com.borqs.server.wutong.ignore.IgnoreLogic;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PeopleYouMayKnowNotifSender extends NotificationSender {
	private String displayNames = "";
    private String nameLinks = "";	
	
	public PeopleYouMayKnowNotifSender() {
		super();
	}

	 @Override
    protected List<Long> getScope(Context ctx, String senderId, Object... args) {
        List<Long> userIds = new ArrayList<Long>();
        userIds.add(Long.parseLong((String) args[0]));
        
      //exclude sender
        if(StringUtils.isNotBlank(senderId))
        {
        	userIds.remove(Long.parseLong(senderId));
        }
        try {
            IgnoreLogic ignore = GlobalLogics.getIgnore();
            userIds = ignore.formatIgnoreUserListP(ctx, userIds, "", "");
        } catch (Exception e) {
        }
        return userIds;
    }

    @Override
    protected String getSettingKey(Context ctx) {
        return Constants.NTF_PEOPLE_YOU_MAY_KNOW;
    }

    @Override
    protected String getAppId(Context ctx, Object... args) {
        return String.valueOf(Constants.APP_TYPE_BPC);
    }

    @Override
    protected String getTitle(Context ctx, String lang, Object... args) {
        return "您有新的好友可能认识，快去看看吧！";
    }

    @Override
    protected String getUri(Context ctx, Object... args) {
    	return "borqs://friends/details?uid=" + receiverId + "&tab=2";
    }

    @Override
    protected String getTitleHtml(Context ctx, String lang, Object... args) {
    	return "您有新的好友可能认识，快去看看吧！";
    }

    @Override
    protected String getBody(Context ctx, Object... args)
	{
		String viewerId = (String)args[0];
    	String ulc =  (String)args[1];
		List<String> l = StringUtils2.splitList(ulc, ",", true);
        AccountLogic account = GlobalLogics.getAccount();
        RecordSet rs = account.getUsers(ctx, viewerId, ulc, "user_id,display_name");
			
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

		return displayNames;
	}
    
    @Override
    protected String getBodyHtml(Context ctx, Object... args)
    {
    	return nameLinks;
    }
}