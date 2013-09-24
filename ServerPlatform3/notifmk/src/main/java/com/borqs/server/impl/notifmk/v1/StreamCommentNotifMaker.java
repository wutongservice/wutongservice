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
import com.borqs.server.platform.feature.comment.Comment;
import com.borqs.server.platform.feature.conversation.ConversationLogic;
import com.borqs.server.platform.feature.ignore.Features;
import com.borqs.server.platform.feature.ignore.IgnoreLogic;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.StreamLogic;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.I18nHelper;
import com.borqs.server.platform.util.sender.notif.Notification;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;


public class StreamCommentNotifMaker {
    private ConversationLogic conversation;
    private IgnoreLogic ignore;
    private AccountLogic account;
    private StreamLogic stream;
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

    public Notification make(Context ctx, Record opts) {

        Comment comment = (Comment) opts.get("comment");
        Notification n = Notification.forSend(MakerTemplates.NOTIF_MY_STREAM_COMMENT, AppIds.WUTONG, "");

        Post post = stream.getPost(ctx, Post.STANDARD_COLUMNS, comment.getTarget().getIdAsLong());


        n.setSenderId(String.valueOf(ctx.getViewer()));
        n.setReplace(true);
        String locale = ctx.getLocale();
        if (StringUtils.isEmpty(locale))
            locale = "zh";
        n.setReceiverId(getScope(ctx, comment, post));
        //Post post = stream.getPost(ctx,Post.STANDARD_COLUMNS,Long.parseLong(comment.id));
        n.setTitle(getTitle(ctx, comment, locale, post));
        n.setTitleHtml(getTitleHtml(ctx, comment, locale, post));
        n.setUri("borqs://stream/comment?id=" + comment.getTarget().id);
        n.setBody(body);
        n.setBodyHtml(body);

        n.setAction("android.intent.action.VIEW");
        return n;
    }

    /**
     * 接收通知范围：
     * 1.所有对该post评论过的人
     * 2.该post的所有to和addto的人
     * 3.不包括ignore
     * 4.如果评论的人跟发post的人是同一个人，那么不接收通知
     *
     * @param ctx
     * @param comment
     * @param post
     * @return
     */
    private String getScope(Context ctx, Comment comment, Post post) {

        Page page = new Page(0, 100);
        int[] b = new int[]{Actions.COMMENT};
        long[] b1 = conversation.getTargetUsers(ctx, comment.getTarget(), b, page);
        receiver = CollectionsHelper.toLongList(b1);


        int[] a = new int[]{Actions.TO};
        long[] a1 = conversation.getTargetUsers(ctx, comment.getTarget(), a, page);
        List<Long> addToList = CollectionsHelper.toLongList(a1);


        int[] c = new int[]{Actions.ADDTO};
        long[] c1 = conversation.getTargetUsers(ctx, Target.forComment(comment.getCommentId()), c, page);
        //compose users
        for (Long l : c1) {
            if (!addToList.contains(l))
                addToList.add(l);
        }
        for (Long l : b1) {
            if (!addToList.contains(l))
                addToList.add(l);
        }


        if (!addToList.contains(Long.valueOf(post.getSourceId()))) {
            addToList.add(post.getSourceId());
        }


        List<Long> list0 = new ArrayList<Long>();
        list0.addAll(addToList);
        //ignore users
        for (Long l : list0) {
            Target[] targets = ignore.getIgnored(ctx, l, Features.STREAM);
            long[] users = Target.getIdsAsLong(targets, Target.USER);
            if (users.length > 0) {
                List<Long> userIds = CollectionsHelper.toLongList(users);
                if (userIds.contains(Long.valueOf(ctx.getViewer())))
                    addToList.remove(l);
            }

        }
        if (addToList.contains(Long.valueOf(ctx.getViewer())))
            addToList.remove(Long.valueOf(ctx.getViewer()));
        return StringUtils.join(addToList, ",");
    }

    private String getTitle(Context ctx, Comment comment, String locale, Post post) {
        long[] userIds = CollectionsHelper.toLongArray(receiver);
        if (userIds.length > 4)
            userIds = ArrayUtils.subarray(userIds, 0, 3);
        Users users = account.getUsers(ctx, User.STANDARD_COLUMNS, userIds);
        String userNames = StringUtils.join(users.getDisplayNames(), ",");


        String title = "";
        if (post != null) {
            body = post.getMessage();
            String title1 = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.notif.streamcomment.title");
            title = userNames + title1 + post.getMessage();
        }

        return title;
    }

    private String getTitleHtml(Context ctx, Comment comment, String locale, Post post) {
        String s = TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.USER, CollectionsHelper.toLongArray(receiver));
        String title1 = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.notif.streamcomment.title");
        s += title1;


        if (StringUtils.isNotEmpty(post.getMessage())) {
            s += ":<a href=\"borqs://stream/comment?id=" + post.getPostId() + "\">" + post.getMessage() + "</a>";
        }

        return s;
    }
}
