package com.borqs.server.wutong.notif;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.ignore.IgnoreImpl;
import com.borqs.server.wutong.ignore.IgnoreLogic;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;


public class RequestAttentionNotifSender extends NotificationSender {
    private ArrayList<Long> toIds = new ArrayList<Long>();

	public RequestAttentionNotifSender() {
		super();
        isReplace = true;
	}

	@Override
	protected List<Long> getScope(Context ctx, String senderId, Object... args) {
		List<Long> scope = new ArrayList<Long>();		
		scope.add(Long.parseLong((String)args[0]));
				
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
		toIds.addAll(scope);
        return scope;
	}

	@Override
	protected String getSettingKey(Context ctx) {
		return Constants.NTF_REQUEST_ATTENTION;
	}
	
	@Override
	protected String getAppId(Context ctx, Object... args)
	{
		return String.valueOf(Constants.APP_TYPE_BPC);
	}
		
	@Override
	protected String getTitle(Context ctx, String lang, Object... args)
	{
		return args[1] + "请求您将他加入您的圈子";
	}
	
	@Override
	protected String getUri(Context ctx, Object... args)
	{		
		return "borqs://profile/details?uid=" + args[0] + "&tab=2";
	}

    @Override
    protected String getData(Context ctx, Object... args) {
        if (toIds.isEmpty())
            return "";
        else
            return "," + StringUtils2.joinIgnoreBlank(",", toIds) + ",";
    }

	@Override
	protected String getTitleHtml(Context ctx, String lang, Object... args)
	{
		return "<a href=\"borqs://profile/details?uid=" + args[0] + "&tab=2\">"+ args[1] + "</a>请求您将他加入您的圈子";
	}
}