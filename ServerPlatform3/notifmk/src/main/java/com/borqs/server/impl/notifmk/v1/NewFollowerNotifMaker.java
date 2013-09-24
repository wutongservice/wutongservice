package com.borqs.server.impl.notifmk.v1;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.TargetInfoFormat;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.app.AppIds;
import com.borqs.server.platform.feature.friend.FriendsHook;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.ignore.Features;
import com.borqs.server.platform.feature.ignore.IgnoreLogic;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.I18nHelper;
import com.borqs.server.platform.util.sender.notif.Notification;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;


public class NewFollowerNotifMaker {

    private IgnoreLogic ignore;
    private AccountLogic account;
    private PeopleIds peopleIds = new PeopleIds();

    public void setAccount(AccountLogic account) {
        this.account = account;
    }


    public void setIgnore(IgnoreLogic ignore) {
        this.ignore = ignore;
    }



    public Notification make(Context ctx, Record opts) {

        ArrayList<FriendsHook.Entry> friend = (ArrayList<FriendsHook.Entry>) opts.get("entry");
        Notification n = Notification.forSend(MakerTemplates.NOTIF_NEW_FOLLOWER, AppIds.WUTONG, "");
        n.setSenderId(String.valueOf(ctx.getViewer()));
        n.setReplace(true);
        String locale = ctx.getLocale();
        if (StringUtils.isEmpty(locale))
            locale = "zh";


        User user = account.getUser(ctx, User.STANDARD_COLUMNS, ctx.getViewer());
        n.setTitle(getTitle(ctx, user));
        n.setTitleHtml(getTitleHtml(ctx, user));
        n.setUri("borqs://userlist/fans?uid=" + ctx.getViewer());
        n.setBody("");
        n.setBodyHtml("");
        n.setReceiverId(getScope(ctx, friend));
        n.setAction("android.intent.action.VIEW");
        return n;
    }

    private String getScope(Context ctx, ArrayList<FriendsHook.Entry> friend) {


        for (FriendsHook.Entry f : friend) {
            peopleIds.add(f.friendId);
        }
        List<PeopleId> list = new ArrayList<PeopleId>();
        list.addAll(peopleIds);

        for (PeopleId l : list) {
            Target[] targets = ignore.getIgnored(ctx, l.getIdAsLong(), Features.STREAM);
            long[] users = Target.getIdsAsLong(targets, Target.USER);
            if (users.length > 0) {
                List<Long> userIds = CollectionsHelper.toLongList(users);
                if (userIds.contains(Long.valueOf(ctx.getViewer())))
                    peopleIds.remove(l);
            }
        }

        return StringUtils.join(peopleIds, ",");
    }

    // 这里需要针对不同的receiver来生成不同的title，暂时不能处理
    private String getTitle(Context ctx, User user) {
        String locale = ctx.getLocale();
        if (StringUtils.isEmpty(locale))
            locale = "zh";

        String title = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.notif.newfollower.title");
        MessageFormat.format(title, user.getDisplayName());
        return title;
    }

    private String getTitleHtml(Context ctx, User user) {
        String locale = ctx.getLocale();
        if (StringUtils.isEmpty(locale))
            locale = "zh";

        String title = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.notif.newfollower.title");
        MessageFormat.format(title, user.getDisplayName());
        String s = TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.USER, new long[]{user.getUserId()});
        return title + s;
    }

}
