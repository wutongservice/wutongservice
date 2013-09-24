package com.borqs.server.impl.notifmk.v1;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.TargetInfoFormat;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.app.AppIds;
import com.borqs.server.platform.feature.ignore.Features;
import com.borqs.server.platform.feature.ignore.IgnoreLogic;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.feature.request.Request;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.I18nHelper;
import com.borqs.server.platform.util.sender.notif.Notification;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * 暂时没有加入相应的api
 */
public class RequestAttentionNotifMaker {

    private IgnoreLogic ignore;
    private AccountLogic account;


    public void setAccount(AccountLogic account) {
        this.account = account;
    }


    public void setIgnore(IgnoreLogic ignore) {
        this.ignore = ignore;
    }


    public Notification make(Context ctx, Record opts) {

        Request rquest = (Request) opts.get("suggest");
        //body = post.getMessage();
        Notification n = Notification.forSend(MakerTemplates.NOTIF_REQUEST_ATTENTION, AppIds.WUTONG, "");
        n.setSenderId(String.valueOf(ctx.getViewer()));
        n.setReplace(true);
        String locale = ctx.getLocale();
        if (StringUtils.isEmpty(locale))
            locale = "zh";

        //Post post = stream.getPost(ctx,Post.STANDARD_COLUMNS,Long.parseLong(comment.id));
        n.setTitle(getTitle(ctx, rquest, locale));
        n.setTitleHtml(getTitleHtml(ctx, rquest, locale));
        n.setUri("borqs://profile/details?uid=" + rquest.getFrom() + "&tab=2");
        n.setBody("");
        n.setBodyHtml("");
        n.setReceiverId(getScope(ctx, rquest));
        n.setAction("android.intent.action.VIEW");
        return n;
    }

    private String getScope(Context ctx, Request request) {

        String l = String.valueOf(request.getTo());
        Target[] targets = ignore.getIgnored(ctx, request.getTo(), Features.SUGGEST);
        long[] users = Target.getIdsAsLong(targets, Target.USER);
        if (users.length > 0) {
            List<Long> userIds = CollectionsHelper.toLongList(users);
            if (userIds.contains(Long.valueOf(ctx.getViewer())))
                return "";
        }
        return l;
    }

    private String getTitle(Context ctx, Request request, String locale) {
        User user = account.getUser(ctx, User.STANDARD_COLUMNS, request.getFrom());
        String title = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.notif.requestattention.titles");
        return user.getDisplayName() + title;
    }

    private String getTitleHtml(Context ctx, Request request, String locale) {
        String title = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.notif.requestattention.titles");

        String s = TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.USER, new String[]{String.valueOf(request.getTo())});

        return s + title;
    }

}
