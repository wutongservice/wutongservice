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
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.StreamLogic;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.I18nHelper;
import com.borqs.server.platform.util.sender.notif.Notification;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;


public class StreamRetweetNotifMaker {
    private ConversationLogic conversation;
    private IgnoreLogic ignore;
    private AccountLogic account;
    private StreamLogic stream;

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

    public Notification make(Context ctx, Record opts) {

        Post post = (Post) opts.get("post");
        Notification n = Notification.forSend(MakerTemplates.NOTIF_MY_STREAM_RETWEET, AppIds.WUTONG, "");
        n.setSenderId(String.valueOf(ctx.getViewer()));
        n.setReceiverId(getScope(ctx, post.getQuote()));
        n.setReplace(true);
        String locale = ctx.getLocale();
        if (StringUtils.isEmpty(locale))
            locale = "zh";

        n.setTitle(getTitle(ctx, post, locale));
        n.setTitleHtml(getTitleHtml(ctx, post, locale));
        n.setUri("borqs://stream/comment?id=" + post.getPostId());
        n.setBody(post.getMessage());
        n.setBodyHtml(post.getMessage());
        n.setAction("android.intent.action.VIEW");
        return n;
    }

    private String getScope(Context ctx, long postId) {


        Page page = new Page(0, 100);
        int[] a = new int[]{Actions.RESHARE,Actions.ADDTO};
        Conversations c = conversation.findByTarget(ctx, null, a, page, Target.forPost(postId));
        List<Long> addToList = CollectionsHelper.toLongList(c.getUsers());

        int[] b = new int[]{Actions.SHARE,Actions.TO};
        Conversations c1 = conversation.findByTarget(ctx, null, b, page, Target.forPost(postId));
        List<Long> postList = CollectionsHelper.toLongList(c1.getUsers());

        //compose users
        for (Long l : addToList) {
            if (!postList.contains(l))
                postList.add(l);
        }

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
        String title = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.notif.retweet.title");

        User user = account.getUser(ctx, User.STANDARD_COLUMNS, post.getSourceId());
        return MessageFormat.format(title, user.getDisplayName(), post.getMessage());
    }

    private String getTitleHtml(Context ctx, Post post, String locale) {
        User user = account.getUser(ctx, User.STANDARD_COLUMNS, post.getSourceId());
        Post postOld = stream.getPost(ctx, Post.STANDARD_COLUMNS, post.getQuote());
        String s = TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.USER, new long[]{user.getUserId()});

        String title = s + " <a href=\"borqs://stream/comment?id="
                + post.getPostId() + "\">引用</a> 了来源于你的分享:<a href=\"borqs://stream/comment?id="
                + post.getQuote() + "\">" + postOld.getMessage() + "</a>";
        return title;
    }
}
