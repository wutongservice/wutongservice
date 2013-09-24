package com.borqs.server.wutong.notif;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.usersugg.SuggestedUserImpl;
import com.borqs.server.wutong.usersugg.SuggestedUserLogic;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;


public class AcceptSuggestNotifSender extends NotificationSender {

	public AcceptSuggestNotifSender() {
		super();
	}

	@Override
	protected List<Long> getScope(Context ctx, String senderId, Object... args) {
		String to = (String)args[0];
		String beSuggested = (String)args[1];
		List<Long> scope = new ArrayList<Long>();

        SuggestedUserLogic suggest = GlobalLogics.getSuggest();
        String s = suggest.getWhoSuggest(ctx, to, beSuggested);
        scope = StringUtils2.splitIntList(s, ",");


		//exclude sender
		if(StringUtils.isNotBlank(senderId))
		{
			scope.remove(Long.parseLong(senderId));
		}
		return scope;
	}

	@Override
	protected String getSettingKey(Context ctx) {
		return Constants.NTF_ACCEPT_SUGGEST;
	}
	
	@Override
	protected String getAppId(Context ctx, Object... args)
	{
		return String.valueOf(Constants.APP_TYPE_BPC);
	}
		
	@Override
	protected String getTitle(Context ctx, String lang, Object... args)
	{		
		return "基于您的推荐，" + args[0] + "和" + args[1] + "成为了好友";
	}
	
	@Override
	protected String getUri(Context ctx, Object... args)
	{
		//TODO
		return "";
	}
	
	@Override
	protected String getTitleHtml(Context ctx, String lang, Object... args)
	{
		return "基于您的推荐，<a href=\"borqs://profile/details?uid=" + args[0] + "&tab=2\">" + args[1]+ "</a>和"
	           + "<a href=\"borqs://profile/details?uid=" + args[2] + "&tab=2\">" + args[3] + "</a>成为了好友";
	}		
}