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
import com.borqs.server.platform.feature.ignore.Features;
import com.borqs.server.platform.feature.ignore.IgnoreLogic;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.feature.photo.PhotoLogic;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.StreamLogic;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.I18nHelper;
import com.borqs.server.platform.util.sender.notif.Notification;
import com.borqs.server.platform.util.template.FreeMarker;
import org.apache.commons.lang.StringUtils;

import java.util.List;


public class SharedAppNotifMaker {
    private ConversationLogic conversation;
    private IgnoreLogic ignore;
    private AccountLogic account;
    private StreamLogic stream;
    private PhotoLogic photo;
    private List<Long> receiver;
    private String body;

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public void setConversation(ConversationLogic conversation) {
        this.conversation = conversation;
    }

    public void setIgnore(IgnoreLogic ignore) {
        this.ignore = ignore;
    }

    public void setStream(StreamLogic stream) {
        this.stream = stream;
    }

    public void setPhoto(PhotoLogic photo) {
        this.photo = photo;
    }

    public Notification make(Context ctx, Record opts) {

        Post post = (Post) opts.get("post");
        body = post.getMessage();
        Notification n = Notification.forSend(MakerTemplates.NOTIF_APP_SHARE, AppIds.WUTONG, "");
        n.setSenderId(String.valueOf(ctx.getViewer()));
        n.setReplace(true);
        String locale = ctx.getLocale();
        if (StringUtils.isEmpty(locale))
            locale = "zh";

        //Post post = stream.getPost(ctx,Post.STANDARD_COLUMNS,Long.parseLong(comment.id));
        n.setTitle(getTitle(ctx, post,locale));
        n.setTitleHtml(getTitleHtml(ctx, post,locale));
        n.setUri("borqs://application/details?id=" + post.getPostTarget().id + "");
        n.setBody(body);
        n.setBodyHtml(body);
        n.setReceiverId(getScope(ctx, post));
        n.setAction("android.intent.action.VIEW");
        return n;
    }

    private String getScope(Context ctx, Post post1) {

        Page page = new Page(0, 100);
        int[] a = new int[]{Actions.TO,Actions.ADDTO};
        long[] a1 = conversation.getTargetUsers(ctx, post1.getPostTarget(), a, page);
        List<Long> addToList = CollectionsHelper.toLongList(a1);
        List<Long> addToList0 = CollectionsHelper.toLongList(a1);

        //ignore users
        for (Long l : addToList) {
            Target[] targets = ignore.getIgnored(ctx, l, Features.STREAM);
            long[] users = Target.getIdsAsLong(targets, Target.USER);
            if (users.length > 0) {
                List<Long> userIds = CollectionsHelper.toLongList(users);
                if (userIds.contains(Long.valueOf(ctx.getViewer())))
                    addToList0.remove(l);
            }
        }

        return StringUtils.join(addToList0, ",");
    }

    private String getTitle(Context ctx, Post post,String locale) {
        User user = account.getUser(ctx, User.STANDARD_COLUMNS, post.getSourceId());
        String title = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.notif.shareapp.title");
        String returnString = user.getDisplayName() + title;
        String id = post.getPostTarget().id;
        String s = new FreeMarker().mergeRaw(returnString + "${all}", new Object[][]{
                {"all", TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.APK, new String[]{id})},
        });

        return s;
    }

    private String getTitleHtml(Context ctx, Post post,String locale) {
        User user = account.getUser(ctx, User.STANDARD_COLUMNS, post.getSourceId());
        String title = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.notif.shareapp.title");
        String userStr = new FreeMarker().mergeRaw("${all}", new Object[][]{
                {"all", TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.USER, new String[]{String.valueOf(user.getUserId())})},
        });
        String l = userStr+title;
        
        String id = post.getPostTarget().id;
        String s = new FreeMarker().mergeRaw(l + "${all}", new Object[][]{
                {"all", TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.APK, new String[]{id})},
        });

        return s;
    }

}
