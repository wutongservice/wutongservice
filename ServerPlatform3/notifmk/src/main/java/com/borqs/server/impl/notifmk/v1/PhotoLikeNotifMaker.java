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
import com.borqs.server.platform.feature.comment.Comment;
import com.borqs.server.platform.feature.conversation.ConversationLogic;
import com.borqs.server.platform.feature.conversation.Conversations;
import com.borqs.server.platform.feature.ignore.Features;
import com.borqs.server.platform.feature.ignore.IgnoreLogic;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.I18nHelper;
import com.borqs.server.platform.util.sender.notif.Notification;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class PhotoLikeNotifMaker {
    private ConversationLogic conversation;
    private IgnoreLogic ignore;
    private AccountLogic account;

    private String displayName = "";

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public void setConversation(ConversationLogic conversation) {
        this.conversation = conversation;
    }

    public void setIgnore(IgnoreLogic ignore) {
        this.ignore = ignore;
    }

    public Notification make(Context ctx, Record opts) {
        String locale = ctx.getLocale();
        if (StringUtils.isEmpty(locale))
            locale = "zh";

        Comment comment = (Comment) opts.get("comment");
        Notification n = Notification.forSend(MakerTemplates.NOTIF_MY_PHOTO_LIKE, AppIds.WUTONG, "");
        n.setReplace(true);
        n.setSenderId(String.valueOf(ctx.getViewer()));
        String photoId = comment.getTarget().id;
        String uri = "borqs://photo/comment?id=" + photoId;

        String tiltleHtml = getTitleHtml(ctx, photoId);
        n.setTitle(getTitle(ctx));

        String commentapp = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "profile.update.notif.likephoto");
        n.setTitleHtml(tiltleHtml + commentapp);

        n.setUri(uri);
        n.setBody(comment.getMessage());
        n.setBodyHtml("");
        n.setReceiverId(getPhotoCommentScope(ctx, comment.getTarget()));
        n.setAction("android.intent.action.VIEW");
        return n;
    }

    private String getTitle(Context ctx) {
        String locale = ctx.getLocale();
        if (StringUtils.isEmpty(locale))
            locale = "zh";

        String likePhoto = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "profile.update.notif.likephoto");

        return displayName += likePhoto;
    }

    public String getTitleHtml(Context ctx, String commentId) {
        List<Long> listWhoComment;
        List<Long> listWhoLike;

        int[] comment = new int[]{Actions.LIKE};
        Page page = new Page(0, 1000);
        Conversations converWhoComment = conversation.findByTarget(ctx, null, comment, page, Target.forComment(Long.parseLong(commentId)));
        long[] addTo = converWhoComment.getUsers();

        listWhoLike = CollectionsHelper.toLongList(addTo);
        listWhoLike = CollectionsHelper.toLongList(addTo);

        for (Long longs : listWhoLike) {
            Target[] t = ignore.getIgnored(ctx, longs, Features.LIKE);

            for (Target target : t) {
                Long l = Long.valueOf(target.id);
                if (listWhoLike.contains(l)) {
                    listWhoLike.remove(l);
                }
            }
        }

        for (Long l : listWhoLike) {
            User user = account.getUser(ctx, User.STANDARD_COLUMNS, l);
            displayName += user.getDisplayName() + ",";
        }
        if (displayName.length() > 0) {
            displayName = StringUtils.substringBeforeLast(displayName, ",");
        }

        String s = TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.USER, CollectionsHelper.toLongArray(listWhoLike));
        return s;
    }

    /**
     * 评论app接收范围
     * 1.所有评论过该app的人
     * 2.ignore
     * 3.如果该post的创建者是本人，不发通知
     *
     * @param ctx
     * @param target1
     * @return
     */
    public String getPhotoCommentScope(Context ctx, Target target1) {
        List<Long> listAll;

        int[] a = new int[]{Actions.LIKE,Actions.SHARE_ATTACHMENTS};
        Page page = new Page(0, 1000);
        Conversations conversations = conversation.findByTarget(ctx, null, a, page, target1);//.findByUser(ctx, null, a, Target.APK, page, ctx.getViewer());
        long[] all = conversations.getUsers();
        listAll = CollectionsHelper.toLongList(all);
        List<Long> listAll0 = CollectionsHelper.toLongList(all);
        //ignore only ignore user
        for (Long userId : listAll0) {
            Target[] t = ignore.getIgnored(ctx, userId, Features.COMMENT);
            for (Target target : t) {
                Long l = Long.valueOf(target.id);
                if (listAll.contains(l)) {
                    listAll.remove(l);
                }
            }
        }

        if (listAll.contains(Long.valueOf(ctx.getViewer())))
            listAll.remove(Long.valueOf(ctx.getViewer()));
        return StringUtils.join(listAll, ",");
    }


}
