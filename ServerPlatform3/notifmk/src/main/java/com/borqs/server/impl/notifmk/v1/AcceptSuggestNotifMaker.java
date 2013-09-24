package com.borqs.server.impl.notifmk.v1;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.TargetInfoFormat;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.app.AppIds;
import com.borqs.server.platform.feature.friend.FriendsHook;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.feature.psuggest.PeopleSuggestLogic;
import com.borqs.server.platform.feature.psuggest.PeopleSuggests;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.I18nHelper;
import com.borqs.server.platform.util.sender.notif.Notification;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class AcceptSuggestNotifMaker {

    private AccountLogic account;
    private PeopleSuggestLogic suggest;

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public void setSuggest(PeopleSuggestLogic suggest) {
        this.suggest = suggest;
    }

    public Notification make(Context ctx, Record opts) {
        String locale = ctx.getLocale();
        if (StringUtils.isEmpty(locale))
            locale = "zh";

        // get the foreigner from ctx
        List<FriendsHook.Entry> entryList = (List<FriendsHook.Entry>) opts.get("entry");
        User userFrom = new User();
        Users usersTo = new Users();
        StringBuilder stringBuilder = new StringBuilder();
        List<Long> users = new ArrayList<Long>();

        // now entryList only contains single case
        for (FriendsHook.Entry entry : entryList) {
            users.add(entry.friendId.getIdAsLong());
            userFrom = account.getUser(ctx, User.STANDARD_COLUMNS, entry.userId);
            User userTo = account.getUser(ctx, User.STANDARD_COLUMNS, entry.friendId.getIdAsLong());
            usersTo.add(userTo);
            String s1 =  TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.USER, new long[]{userTo.getUserId()});
            stringBuilder.append(s1);
        }
        String s2 = TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.USER, new long[]{userFrom.getUserId()});
        String s = TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.USER, CollectionsHelper.toLongArray(users));
        String title1 = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.notif.acceptsuggest.title");

        String titleHtml = MessageFormat.format(title1, s2, s);

        Notification n = Notification.forSend(MakerTemplates.NOTIF_ACCEPT_SUGGEST, AppIds.WUTONG, "");
        n.setReplace(false);

        String title = MessageFormat.format(title1, userFrom.getDisplayName(), StringUtils.join(usersTo.getDisplayNames(), ","));

        n.setTitle(title);
        n.setTitleHtml(titleHtml);


        n.setData("");

        n.setSenderId(findWhoSuggested(ctx,userFrom,usersTo.get(0)));

        n.setUri("");
        n.setBody("");
        n.setBodyHtml("");
        n.setReceiverId(StringUtils.join(usersTo, ","));
        n.setAction("android.intent.action.VIEW");
        return n;
    }

    private String findWhoSuggested(Context ctx,User userFrom,User userTo) {
        if(userFrom == null || userTo == null)
            return "";

        PeopleSuggests ps = suggest.getPeopleSource(ctx, userFrom.getUserId(),userTo.getUserId());
        if (ps.size() > 0)
            return ps.get(0).getSource();
        return "";
    }
}
