package com.borqs.server.service.notification;

import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;


public class RequestAttentionNotifSender extends NotificationSender {

	public RequestAttentionNotifSender(Platform p, Qiupu qiupu) {
		super(p, qiupu);
        isReplace = true;
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
           scope = p.formatIgnoreUserList(senderId, scope, "","");
        } catch (Exception e) {
        }
		return scope;
	}

	@Override
	protected String getSettingKey() {
		return Constants.NTF_REQUEST_ATTENTION;
	}
	
	@Override
	protected String getAppId(Object... args)
	{
		return String.valueOf(Constants.APP_TYPE_BPC);
	}
		
	@Override
	protected String getTitle(Object... args)
	{
		return args[1] + "请求您将他加入您的圈子";
	}
	
	@Override
	protected String getUri(Object... args)
	{		
		return "borqs://profile/details?uid=" + args[0] + "&tab=2";
	}
	
	@Override
	protected String getTitleHtml(Object... args)
	{
		return "<a href=\"borqs://profile/details?uid=" + args[0] + "&tab=2\">"+ args[1] + "</a>请求您将他加入您的圈子";
	}
}