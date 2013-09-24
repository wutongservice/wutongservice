package com.borqs.server.impl.notifmk.v1;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.TargetInfoFormat;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.app.AppIds;
import com.borqs.server.platform.feature.conversation.ConversationLogic;
import com.borqs.server.platform.feature.ignore.Features;
import com.borqs.server.platform.feature.ignore.IgnoreLogic;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.StreamLogic;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.I18nHelper;
import com.borqs.server.platform.util.sender.notif.Notification;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;


public class StreamLikeNotifMaker {
    private ConversationLogic conversation;
    private IgnoreLogic ignore;
    private AccountLogic account;
    private StreamLogic stream;

    List<Long> postList;


    public void setConversation(ConversationLogic conversation) {
        this.conversation = conversation;
    }

    public void setIgnore(IgnoreLogic ignore) {
        this.ignore = ignore;
    }

    public void setStream(StreamLogic stream) {
        this.stream = stream;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public Notification make(Context ctx, Record opts) {

        Target target = (Target) opts.get("like");
        Notification n = Notification.forSend(MakerTemplates.NOTIF_MY_STREAM_LIKE, AppIds.WUTONG, "");
        n.setSenderId(String.valueOf(ctx.getViewer()));
        n.setReplace(true);
        String locale = ctx.getLocale();
        if (StringUtils.isEmpty(locale))
            locale = "zh";

        Post post = stream.getPost(ctx, Post.STANDARD_COLUMNS, Long.parseLong(target.id));
        n.setReceiverId(getScope(ctx, Long.parseLong(target.id),post));
        n.setTitle(getTitle(ctx, post, locale));
        n.setTitleHtml(getTitleHtml(ctx, post, locale));
        n.setUri("borqs://stream/comment?id=" + target.id);

        String body = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.like.like");
        
        n.setBody(body);
        n.setBodyHtml(body);

        n.setAction("android.intent.action.VIEW");
        return n;
    }

    private String getScope(Context ctx, long postId,Post p) {

        Page page = new Page(0, 100);

        // get the sender who liked this post
        int[] b = new int[]{Actions.LIKE};
        long[] c1 = conversation.getTargetUsers(ctx, Target.forPost(postId), b, page);

        postList = CollectionsHelper.toLongList(c1);
        postList.add(0,ctx.getViewer());
        //compose users

        if (!postList.contains( p.getSourceId()))
            postList.add( p.getSourceId());


        if (postList.contains(Long.valueOf(ctx.getViewer())))
            postList.remove(Long.valueOf(ctx.getViewer()));

        List<Long> list0 = new ArrayList<Long>();
        list0.addAll(postList);
        //ignore users
        for (Long l : list0) {
            Target[] targets = ignore.getIgnored(ctx, l, Features.STREAM);
            long[] users = Target.getIdsAsLong(targets, Target.USER);
            if (users.length > 0) {
                List<Long> userIds = CollectionsHelper.toLongList(users);
                if (userIds.contains(Long.valueOf(ctx.getViewer())))
                    postList.remove(l);
            }

        }


        return StringUtils.join(postList, ",");
    }

    private String getTitle(Context ctx, Post post, String locale) {
        if (postList == null)
            return "";

        List<Long> list = new ArrayList<Long>();
        if (postList.size() > 4) {
            list.addAll(postList.subList(0, 3));
        }else{
            list.addAll(postList);
        }

        Users users = account.getUsers(ctx, User.STANDARD_COLUMNS, CollectionsHelper.toLongArray(list));
        String displayName = StringUtils.join(users.getDisplayNames(), ",");

        String title = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.notif.streamlike.title");

        return displayName + title + post.getMessage();
    }

    private String getTitleHtml(Context ctx, Post post, String locale) {
        String s =  TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.USER, CollectionsHelper.toLongArray(postList));
        String title1 = ":<a href=\"borqs://stream/comment?id=" + post.getPostId() + "\">" + post.getMessage() + "</a>";
        String title = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.notif.streamlike.title");
        return s + title + title1;
    }
}
