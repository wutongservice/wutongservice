package com.borqs.server.service.notification;

import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;


public class ReportAbuseNotifSender extends NotificationSender {

	public ReportAbuseNotifSender(Platform p, Qiupu qiupu) {
		super(p, qiupu);		
	}

	@Override
	protected List<Long> getScope(String senderId, Object... args) {		
		List<Long> scope = new ArrayList<Long>();		
		scope.add(Long.parseLong((String)args[1]));
		return scope;
	}

	@Override
	protected String getSettingKey() {
		return Constants.NTF_REPORT_ABUSE;
	}
	
	@Override
	protected String getAppId(Object... args)
	{
		return String.valueOf(Constants.APP_TYPE_BPC);
	}

    @Override
    protected String getTitle(Object... args) {
        String message = args[2].toString();
        if (message.length() > 50) {
            message = StringUtils.substring(args[2].toString(), 0, 50) + "...";
        }
        if (StringUtils.isNotBlank(message)) {
            message = "<a href=\"borqs://stream/comment?id=" + args[0] + "\">" + message + "</a>";
        }
        return "您举报的动态：" + message + " 梧桐管理员已经接收,正在处理中";
    }

    @Override
    protected String getUri(Object... args) {
        return "borqs://stream/comment?id="+(String)args[0];
    }

    @Override
    protected String getTitleHtml(Object... args) {
        String message = args[2].toString();

        if (message.length() > 50) {
            message = StringUtils.substring(args[2].toString(), 0, 50) + "...";
        }
        if (StringUtils.isNotBlank(message)) {
            message = "<a href=\"borqs://stream/comment?id=" + args[0] + "\">" + message + "</a>";
        }
        return "您举报的动态：" + message + " 梧桐管理员已经接收,正在处理中";
    }
}