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


public class SuggestUserNotifSender extends NotificationSender {
    private ArrayList<Long> toIds = new ArrayList<Long>();

	public SuggestUserNotifSender() {
		super();
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
		return Constants.NTF_SUGGEST_USER;
	}
	
	@Override
	protected String getAppId(Context ctx, Object... args)
	{
		return String.valueOf(Constants.APP_TYPE_BPC);
	}
		
	@Override
	protected String getTitle(Context ctx, String lang, Object... args)
	{
        int count = Integer.parseInt((String)args[2]);
        if(count == 1)
            return args[0] + "向您推荐了好友" + args[1];
        else
            return args[0] + "向您推荐了" + args[1] + "等" + args[2] + "个好友";
	}
	
	@Override
	protected String getUri(Context ctx, Object... args)
	{		
		return "borqs://friends/details?uid=" + receiverId + "&tab=2";
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
        int count = Integer.parseInt((String)args[4]);
        if(count == 1)
            return "<a href=\"borqs://profile/details?uid=" + args[0] + "&tab=2\">"+ args[1] + "</a>向您推荐了好友"
                    + "<a href=\"borqs://profile/details?uid=" + args[2] + "&tab=2\">" + args[3] + "</a>";
        return "<a href=\"borqs://profile/details?uid=" + args[0] + "&tab=2\">"+ args[1] + "</a>向您推荐了"
	           + "<a href=\"borqs://profile/details?uid=" + args[2] + "&tab=2\">" + args[3] + "</a>等" + args[4] + "个好友";
	}
}