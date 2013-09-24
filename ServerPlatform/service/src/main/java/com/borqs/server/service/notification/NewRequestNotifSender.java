package com.borqs.server.service.notification;

import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;


public class NewRequestNotifSender extends NotificationSender {

	public NewRequestNotifSender(Platform p, Qiupu qiupu) {
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
		return Constants.NTF_NEW_REQUEST;
	}
	
	@Override
	protected String getAppId(Object... args)
	{
		return String.valueOf(Constants.APP_TYPE_BPC);
	}

	@Override
	protected String getTitle(Object... args)
	{
//		int count = Integer.parseInt((String)args[1]);
//		if(count == 1)
//			return args[0] + "邀请你交换名片";
//		else
//			return args[0] + "等" + args[1] + "个人邀请你交换名片";

        return "你有新的请求，快去看看吧";
	}
	
	@Override
	protected String getUri(Object... args)
	{		
		return "borqs://request/details?uid=" + args[0];
	}
	
	@Override
	protected String getTitleHtml(Object... args)
	{
//		int count = Integer.parseInt((String)args[2]);
//		if(count == 1)
//			return "<a href=\"borqs://profile/details?uid=" + args[0] + "&tab=2\">"+ args[1] + "</a>邀请你交换名片";
//		else
//			return "<a href=\"borqs://profile/details?uid=" + args[0] + "&tab=2\">"+ args[1] + "</a>等" + args[2] + "个人邀请你交换名片";

        return "你有新的请求，快去看看吧";
    }

    @Override
    protected String getBody(Object... args) {
        String type = (String)args[0];
        if (StringUtils.equals(type, Constants.REQUEST_PROFILE_ACCESS)) {
            int count = Integer.parseInt((String) args[2]);
            if (count == 1)
                return args[1] + "邀请你交换名片";
            else
                return args[1] + "等" + args[2] + "个人邀请你交换名片";
        }
        else if(StringUtils.equals(type, Constants.REQUEST_CHANGE_EMAIL_ADDRESS)
                || StringUtils.equals(type, Constants.REQUEST_CHANGE_EMAIL_2_ADDRESS)
                || StringUtils.equals(type, Constants.REQUEST_CHANGE_EMAIL_3_ADDRESS)) {
            return args[1] + "建议你修改电子邮件";
        }
        else {
            return args[1] + "建议你修改手机号码";
        }
    }
}