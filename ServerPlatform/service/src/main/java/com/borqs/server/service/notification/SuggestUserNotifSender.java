package com.borqs.server.service.notification;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.qiupu.Qiupu;


public class SuggestUserNotifSender extends NotificationSender {

	public SuggestUserNotifSender(Platform p, Qiupu qiupu) {
		super(p, qiupu);		
	}

	@Override
	protected List<Long> getScope(String senderId, Object... args) {		
		List<Long> scope = new ArrayList<Long>();		
		scope.add(Long.parseLong((String)args[0]));
				
		//exclude sender
        if(StringUtils.isNotBlank(senderId))
        {
        	scope.remove(Long.parseLong(senderId));
        }
        try {
            scope = p.formatIgnoreUserList(senderId, scope, "", "");
        } catch (Exception e) {
        }
		return scope;
	}

	@Override
	protected String getSettingKey() {
		return Constants.NTF_SUGGEST_USER;
	}
	
	@Override
	protected String getAppId(Object... args)
	{
		return String.valueOf(Constants.APP_TYPE_BPC);
	}
		
	@Override
	protected String getTitle(Object... args)
	{
        int count = Integer.parseInt((String)args[2]);
        if(count == 1)
            return args[0] + "向您推荐了好友" + args[1];
        else
            return args[0] + "向您推荐了" + args[1] + "等" + args[2] + "个好友";
	}
	
	@Override
	protected String getUri(Object... args)
	{		
		return "borqs://friends/details?uid=" + receiverId + "&tab=2";
	}
	
	@Override
	protected String getTitleHtml(Object... args)
	{
        int count = Integer.parseInt((String)args[4]);
        if(count == 1)
            return "<a href=\"borqs://profile/details?uid=" + args[0] + "&tab=2\">"+ args[1] + "</a>向您推荐了好友"
                    + "<a href=\"borqs://profile/details?uid=" + args[2] + "&tab=2\">" + args[3] + "</a>";
        return "<a href=\"borqs://profile/details?uid=" + args[0] + "&tab=2\">"+ args[1] + "</a>向您推荐了"
	           + "<a href=\"borqs://profile/details?uid=" + args[2] + "&tab=2\">" + args[3] + "</a>等" + args[4] + "个好友";
	}
}