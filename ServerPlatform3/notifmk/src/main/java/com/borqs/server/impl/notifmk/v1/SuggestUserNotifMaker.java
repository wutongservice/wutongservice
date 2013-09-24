package com.borqs.server.impl.notifmk.v1;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.TargetInfoFormat;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.app.AppIds;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.ignore.Features;
import com.borqs.server.platform.feature.ignore.IgnoreLogic;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.feature.psuggest.PeopleSuggest;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.I18nHelper;
import com.borqs.server.platform.util.StringHelper;
import com.borqs.server.platform.util.sender.notif.Notification;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.List;


public class SuggestUserNotifMaker {

    private IgnoreLogic ignore;
    private AccountLogic account;


    public void setAccount(AccountLogic account) {
        this.account = account;
    }


    public void setIgnore(IgnoreLogic ignore) {
        this.ignore = ignore;
    }


    public Notification make(Context ctx, Record opts) {

        PeopleSuggest[] suggests = (PeopleSuggest[]) opts.get("suggest");
        //body = post.getMessage();
        Notification n = Notification.forSend(MakerTemplates.NOTIF_SUGGEST_USER, AppIds.WUTONG, "");
        n.setSenderId(String.valueOf(ctx.getViewer()));
        n.setReceiverId(getScope(ctx, suggests));
        n.setReplace(true);
        String locale = ctx.getLocale();
        if (StringUtils.isEmpty(locale))
            locale = "zh";

        //Post post = stream.getPost(ctx,Post.STANDARD_COLUMNS,Long.parseLong(comment.id));
        n.setTitle(getTitle(ctx, suggests, locale));
        n.setTitleHtml(getTitleHtml(ctx, suggests, locale));
        n.setUri("borqs://friends/details?uid=" + getScope(ctx, suggests) + "&tab=2");
        n.setBody("");
        n.setBodyHtml("");
        n.setAction("android.intent.action.VIEW");
        return n;
    }

    private String getScope(Context ctx, PeopleSuggest[] ps) {

        PeopleIds peopleIds = new PeopleIds();
        for (PeopleSuggest p : ps) {
            peopleIds.add(p.getSuggested());
        }
        PeopleIds peopleIds1 = new PeopleIds();
        peopleIds1.addAll(peopleIds);
        //ignore users
        for (PeopleId l : peopleIds1) {
            Target[] targets = ignore.getIgnored(ctx, l.getIdAsLong(), Features.SUGGEST);
            long[] users = Target.getIdsAsLong(targets, Target.USER);
            if (users.length > 0) {
                List<Long> userIds = CollectionsHelper.toLongList(users);
                if (userIds.contains(Long.valueOf(ctx.getViewer())))
                    peopleIds.remove(l);
            }
        }

        return StringUtils.join(peopleIds, ",");
    }

    private String getTitle(Context ctx, PeopleSuggest[] suggests, String locale) {


        int count = suggests.length;
        if (count < 1)
            return "";

        User user0 = account.getUser(ctx, User.STANDARD_COLUMNS, Long.valueOf(suggests[0].getSource()));
        String userName = user0.getDisplayName();

        User user = account.getUser(ctx, User.STANDARD_COLUMNS, suggests[0].getSuggested().getIdAsLong());

        if (count == 1) {
            String title = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.notif.suggestuser.title");
            return userName + MessageFormat.format(title, user.getDisplayName());

        } else {
            String title = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.notif.suggestuser.titles");
            return userName + MessageFormat.format(title, user.getDisplayName(), count);
        }
    }

    private String getTitleHtml(Context ctx, PeopleSuggest[] suggests, String locale) {
        int count = suggests.length;
        if (count < 1)
            return "";

        Users users = account.getUsers(ctx, User.STANDARD_COLUMNS, StringHelper.splitLongArray(suggests[0].getSource(), ","));

        User user = account.getUser(ctx, User.STANDARD_COLUMNS, suggests[0].getSuggested().getIdAsLong());

        if (count == 1) {
            String s = TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.USER, users.getUserIds());
            String s1 = TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.USER, new long[]{user.getUserId()});

            String title = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.notif.suggestuser.title");
            return s + MessageFormat.format(title, s1);

        } else {
            String s = TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.USER, users.getUserIds());

            String s1 = TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.USER, new long[]{user.getUserId()});
            String title = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.notif.suggestuser.titles");
            return s + MessageFormat.format(title, s1, suggests.length);
        }
    }

}
