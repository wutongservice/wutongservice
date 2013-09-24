package com.borqs.server.impl.notifmk.v1;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.TargetInfoFormat;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.app.AppIds;
import com.borqs.server.platform.feature.friend.FriendLogic;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.feature.setting.SettingLogic;
import com.borqs.server.platform.feature.status.Status;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.I18nHelper;
import com.borqs.server.platform.util.sender.notif.Notification;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.List;


public class UpdateStatusNotifMaker {

    private FriendLogic friend;
    private SettingLogic setting;
    private AccountLogic account;

    private String body;

    public void setFriend(FriendLogic friend) {
        this.friend = friend;
    }

    public void setSetting(SettingLogic setting) {
        this.setting = setting;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public Notification make(Context ctx, Record opts) {
        String locale = ctx.getLocale();
        if (StringUtils.isEmpty(locale))
            locale = "zh";

        Status user0 = (Status) opts.get("status");
        getUpdateColumn(user0, locale);
        User user = account.getUser(ctx, User.STANDARD_COLUMNS, ctx.getViewer());
        Notification n = Notification.forSend(MakerTemplates.NOTIF_UPDATE_PROFILE, AppIds.WUTONG, "");
        n.setSenderId(String.valueOf(user.getUserId()));
        n.setReplace(true);

        String titil = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "profile.update.notif.title");
        titil = MessageFormat.format(titil, user.getDisplayName());
        n.setTitle(titil);
        n.setUri("borqs://profile/details?uid=" + user.getUserId() + "&tab=" + 2);

        String titleHtml = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "profile.update.notif.title");
        n.setTitleHtml(MessageFormat.format(titleHtml, TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.USER, new long[]{user.getUserId()})));
        n.setBody(body);
        n.setBodyHtml("");
        n.setReceiverId(getReceiveIds(ctx));
        n.setAction("android.intent.action.VIEW");
        return n;
    }

    public String getReceiveIds(Context ctx) {
        Page page = new Page(0, 1000);
        long[] followers = friend.getFollowers(ctx, PeopleId.user(ctx.getViewer()), page);
        List<Long> list = CollectionsHelper.toLongList(followers);
        for (long l : followers) {
            String value = setting.get(ctx, l, MakerTemplates.NOTIF_UPDATE_PROFILE, "");
            if ("1".equals(value))
                list.remove(l);
        }
        list.remove(ctx.getViewer());
        return StringUtils.join(list, ",");
    }


    private String getUpdateColumn(Status status0, String lang) {
        String status = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.status");
        body = status + ": " + status0.status;
        return status;
    }
}
