package com.borqs.server.impl.notifmk.v1;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.TargetInfoFormat;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.app.AppIds;
import com.borqs.server.platform.feature.conversation.ConversationLogic;
import com.borqs.server.platform.feature.conversation.Conversations;
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

public class AppLikeNotifMaker {
    private ConversationLogic conversation;
    private IgnoreLogic ignore;
    private AccountLogic account;


    public void setConversation(ConversationLogic conversation) {
        this.conversation = conversation;
    }

    public void setIgnore(IgnoreLogic ignore) {
        this.ignore = ignore;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public Notification make(Context ctx, Record opts) {
        String locale = ctx.getLocale();
        if (StringUtils.isEmpty(locale))
            locale = "zh";

        Target target = (Target) opts.get("like");
        Notification n = Notification.forSend(MakerTemplates.NOTIF_MY_APP_LIKE, AppIds.QIUPU, "");

        n.setReplace(true);


        String title = getTitle(ctx, target, locale);

        n.setTitle(title);
        n.setTitleHtml(getTitleHtml(ctx, target));
        n.setData("");

        n.setSenderId(String.valueOf(ctx.getViewer()));

        n.setUri("borqs://application/details?id=" + target.id + "");
        String like = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.like.like");
        n.setBody(like);
        n.setBodyHtml(like);
        n.setReceiverId(getAppLikeScope(ctx, target));
        n.setAction("android.intent.action.VIEW");
        return n;
    }

    private String getAppLikeScope(Context ctx, Target target1) {
        int[] a = new int[]{Actions.SHARE_ATTACHMENTS};
        Page page = new Page(0, 1000);
        Conversations conversations = conversation.findByTarget(ctx, null, a, page, target1);

        long[] all = conversations.getUsers();
        List<Long> listAll = CollectionsHelper.toLongList(all);
        List<Long> listAll0 = CollectionsHelper.toLongList(all);
        //ignore only ignore user
        for (Long userId : listAll0) {
            Target[] t = ignore.getIgnored(ctx, userId, Features.STREAM);
            for (Target target : t) {
                Long l = Long.valueOf(target.id);
                if (listAll.contains(l)) {
                    listAll.remove(l);
                }
            }
        }

        return StringUtils.join(listAll, ",");
    }

    public String getTitleHtml(Context ctx, Target targets) {
        List<Long> listWhoLike0;
        List<Long> listWhoLike;

        int[] reason = new int[]{Actions.LIKE};
        Page page = new Page(0, 1000);
        Conversations converWhoLike = conversation.findByTarget(ctx, null, reason, page, targets);
        long[] addTo = converWhoLike.getUsers();

        listWhoLike0 = CollectionsHelper.toLongList(addTo);
        listWhoLike = CollectionsHelper.toLongList(addTo);

        for (Long longs : listWhoLike0) {

            Target[] t = ignore.getIgnored(ctx, longs, Features.APK);
            for (Target target : t) {
                Long l = Long.valueOf(target.id);
                if (listWhoLike0.contains(l)) {
                    listWhoLike.remove(l);
                }
            }
        }


        String s = TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.USER, CollectionsHelper.toLongArray(listWhoLike));
        return s;
    }

    private String getTitle(Context ctx, Target target, String locale) {
        int[] reason = new int[]{Actions.LIKE};
        Page page = new Page(0, 4);
        Conversations converWhoLike = conversation.findByTarget(ctx, null, reason, page, target);

        long[] addTo = converWhoLike.getUsers();

        List<String> names = new ArrayList<String>();
        for (long l : addTo) {
            User user = account.getUser(ctx, User.STANDARD_COLUMNS, l);
            names.add(user.getDisplayName());
        }

        String s = TargetInfoFormat.ANDROID_LINK.findTargetInfo(ctx, Target.APK, new String[]{target.id}).getName();

        String title = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.notif.applike.title");
        return MessageFormat.format(title, StringUtils.join(names, ", "), s);
    }
}
