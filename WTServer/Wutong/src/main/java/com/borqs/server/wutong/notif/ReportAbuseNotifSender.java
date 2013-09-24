package com.borqs.server.wutong.notif;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.Constants;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;


public class ReportAbuseNotifSender extends NotificationSender {
    private ArrayList<Long> toIds = new ArrayList<Long>();

	public ReportAbuseNotifSender() {
		super();
	}

	@Override
	protected List<Long> getScope(Context ctx, String senderId, Object... args) {
		List<Long> scope = new ArrayList<Long>();		
		scope.add(Long.parseLong((String)args[1]));
		toIds.addAll(scope);
        return scope;
	}

	@Override
	protected String getSettingKey(Context ctx) {
		return Constants.NTF_REPORT_ABUSE;
	}
	
	@Override
	protected String getAppId(Context ctx, Object... args)
	{
		return String.valueOf(Constants.APP_TYPE_BPC);
	}

    @Override
    protected String getTitle(Context ctx, String lang, Object... args) {
        String message = args[2].toString();
        if (message.length() > 50) {
            message = StringUtils.substring(args[2].toString(), 0, 50) + "...";
        }
        int target_type = Integer.valueOf(args[3].toString());
        String r_message = "";
        if (target_type == Constants.POST_OBJECT) {
            if (StringUtils.isNotBlank(message)) {
                message = "<a href=\"borqs://stream/comment?id=" + args[0] + "\">" + message + "</a>";
            }
            r_message = "您举报的动态：" + message + " 梧桐管理员已经接收,正在处理中";
        }
        if (target_type == Constants.PHOTO_OBJECT) {
            if (StringUtils.isNotBlank(message)) {
                message = "<a href=\"borqs://photo/comment?id=" + args[0] + "\">" + message + "</a>";
            }
            r_message = "您举报的照片：" + message + " 梧桐管理员已经接收,正在处理中";
        }
        if (target_type == Constants.FILE_OBJECT) {
            if (StringUtils.isNotBlank(message)) {
                message = "<a href=\"borqs://file/comment?id=" + args[0] + "\">" + message + "</a>";
            }
            r_message = "您举报的文件：" + message + " 梧桐管理员已经接收,正在处理中";
        }

        return r_message;
    }

    @Override
    protected String getUri(Context ctx, Object... args) {
        return "borqs://stream/comment?id="+(String)args[0];
    }

    @Override
    protected String getData(Context ctx, Object... args) {
        if (toIds.isEmpty())
            return "";
        else
            return "," + StringUtils2.joinIgnoreBlank(",", toIds) + ",";
    }

    @Override
    protected String getTitleHtml(Context ctx, String lang, Object... args) {
        String message = args[2].toString();
        int target_type = Integer.valueOf(args[3].toString());
        String r_message = "";
        if (target_type == Constants.POST_OBJECT) {
            if (StringUtils.isNotBlank(message)) {
                message = "<a href=\"borqs://stream/comment?id=" + args[0] + "\">" + message + "</a>";
            }
            r_message = "您举报的动态：" + message + " 梧桐管理员已经接收,正在处理中";
        }
        if (target_type == Constants.PHOTO_OBJECT) {
            if (StringUtils.isNotBlank(message)) {
                message = "<a href=\"borqs://photo/comment?id=" + args[0] + "\">" + message + "</a>";
            }
            r_message = "您举报的照片：" + message + " 梧桐管理员已经接收,正在处理中";
        }
        if (target_type == Constants.FILE_OBJECT) {
            if (StringUtils.isNotBlank(message)) {
                message = "<a href=\"borqs://file/comment?id=" + args[0] + "\">" + message + "</a>";
            }
            r_message = "您举报的文件：" + message + " 梧桐管理员已经接收,正在处理中";
        }
        return r_message;
    }
}