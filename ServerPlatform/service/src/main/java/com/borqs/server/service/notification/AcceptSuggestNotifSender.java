package com.borqs.server.service.notification;

import java.util.ArrayList;
import java.util.List;

import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;

import com.borqs.server.base.data.RecordSet;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.qiupu.Qiupu;


public class AcceptSuggestNotifSender extends NotificationSender {

	public AcceptSuggestNotifSender(Platform p, Qiupu qiupu) {
		super(p, qiupu);		
	}

	@Override
	protected List<Long> getScope(String senderId, Object... args) {		
		String to = (String)args[0];
		String beSuggested = (String)args[1];
		List<Long> scope = new ArrayList<Long>();
		
		try {			
			scope = p.getWhoSuggest(to, beSuggested);
		} catch (AvroRemoteException e) {			
			e.printStackTrace();
		}
		
		//exclude sender
		if(StringUtils.isNotBlank(senderId))
		{
			scope.remove(Long.parseLong(senderId));
		}
		return scope;
	}

	@Override
	protected String getSettingKey() {
		return Constants.NTF_ACCEPT_SUGGEST;
	}
	
	@Override
	protected String getAppId(Object... args)
	{
		return String.valueOf(Constants.APP_TYPE_BPC);
	}
		
	@Override
	protected String getTitle(Object... args)
	{		
		return "基于您的推荐，" + args[0] + "和" + args[1] + "成为了好友";
	}
	
	@Override
	protected String getUri(Object... args)
	{
		//TODO
		return "";
	}
	
	@Override
	protected String getTitleHtml(Object... args)
	{
		return "基于您的推荐，<a href=\"borqs://profile/details?uid=" + args[0] + "&tab=2\">" + args[1]+ "</a>和"
	           + "<a href=\"borqs://profile/details?uid=" + args[2] + "&tab=2\">" + args[3] + "</a>成为了好友";
	}		
}