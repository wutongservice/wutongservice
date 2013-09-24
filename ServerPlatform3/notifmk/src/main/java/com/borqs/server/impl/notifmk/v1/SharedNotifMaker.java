package com.borqs.server.impl.notifmk.v1;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.TargetInfoFormat;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.app.AppIds;
import com.borqs.server.platform.feature.conversation.ConversationLogic;
import com.borqs.server.platform.feature.ignore.Features;
import com.borqs.server.platform.feature.ignore.IgnoreLogic;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.feature.photo.Photo;
import com.borqs.server.platform.feature.photo.PhotoLogic;
import com.borqs.server.platform.feature.photo.Photos;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.StreamLogic;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.platform.util.sender.notif.Notification;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.node.ArrayNode;

import java.util.List;


public class SharedNotifMaker {
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
        int type = getApps(post);
        Notification n = Notification.forSend(MakerTemplates.NOTIF_OTHER_SHARE, type, "");
        n.setSenderId(String.valueOf(ctx.getViewer()));
        n.setReplace(true);
        String locale = ctx.getLocale();
        if (StringUtils.isEmpty(locale))
            locale = "zh";

        //Post post = stream.getPost(ctx,Post.STANDARD_COLUMNS,Long.parseLong(comment.id));
        n.setTitle(getTitle(ctx, post));
        n.setTitleHtml(getTitleHtml(ctx, post));
        n.setUri("borqs://stream/comment?id=" + post.getPostId());
        n.setBody(body(ctx, post));
        n.setBodyHtml(body);
        n.setReceiverId(getScope(ctx, post));
        n.setAction("android.intent.action.VIEW");
        return n;
    }

    private int getApps(Post post) {
        int type = post.getType();
        if (type == Post.POST_APK || type == Post.POST_APK_LINK ||
                type == (Post.POST_APK | Post.POST_COMMENT_BROADCAST) ||
                type == (Post.POST_APK | Post.POST_LIKE_BROADCAST)) {
            return AppIds.QIUPU;
        }else if(type == Post.POST_BOOK || type == (Post.POST_BOOK ) ||
                type == (Post.POST_BOOK | Post.POST_COMMENT_BROADCAST) ||
                type == (Post.POST_BOOK | Post.POST_LIKE_BROADCAST)){
            return AppIds.BROOK;
        }else{
            return AppIds.WUTONG;
        }
    }

    private String getScope(Context ctx, Post post1) {

        Page page = new Page(0, 100);
        int[] a = new int[]{Actions.TO,Actions.ADDTO};
        long[] a1 = conversation.getTargetUsers(ctx, post1.getPostTarget(), a, page);
        List<Long> addToList = CollectionsHelper.toLongList(a1);
        List<Long> addToList0 = CollectionsHelper.toLongList(a1);

        //ignore users
        for (Long l : addToList0) {
            Target[] targets = ignore.getIgnored(ctx, l, Features.STREAM);
            long[] users = Target.getIdsAsLong(targets, Target.USER);
            if (users.length > 0) {
                List<Long> userIds = CollectionsHelper.toLongList(users);
                if (userIds.contains(Long.valueOf(ctx.getViewer())))
                    addToList.remove(l);
            }
        }

        return StringUtils.join(addToList, ",");
    }

    private String getTitle(Context ctx, Post post) {
        String sType = "动态";
        int type = post.getType();
        if (type == Post.POST_LINK)
            sType = "链接";
        else if (type == Post.POST_APK_LINK)
            sType = "应用链接";
        else if (type == Post.POST_BOOK)
            sType = "图书";
        else if (type == Post.POST_TEXT)
            sType = "消息";
        return sType;
    }

    private String getTitleHtml(Context ctx, Post post) {
        int type = post.getType();
        String sType = "动态";
        if (type == Post.POST_LINK)
            sType = "链接";
        else if (type == Post.POST_APK_LINK)
            sType = "应用链接";
        else if (type == Post.POST_BOOK)
            sType = "图书";
        else if (type == Post.POST_TEXT)
            sType = "消息";

        String s =  TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.USER, new long[]{post.getSourceId()});

        return s + "给您分享了他的" + sType;
    }

    private String body(Context ctx, Post post) {
        int type = post.getType();

        String attachments = post.getAttachments();

        if ((type == Post.POST_APK_LINK)) {

            ArrayNode aNode = (ArrayNode) JsonHelper.parse(attachments);
            if ((aNode != null) && (aNode.get(0) != null))
                body = aNode.get(0).get("href").getTextValue();
        } else if (type == Post.POST_BOOK) {

            if (attachments.length() >= 2) {
                ArrayNode aNode = (ArrayNode) JsonHelper.parse(attachments);
                if ((aNode != null) && (aNode.get(0) != null))
                    body = aNode.get(0).get("summary").getTextValue();
            }
        } else if (type == Post.POST_PHOTO) {
            if (post.getAttachmentIds().length < 1)
                return "";

            // get the first record of photo array
            Photos photos = photo.getPhotos(ctx, post.getAttachmentTargetIds()[0].getIdAsLong());
            if (photos.size() < 1)
                return "";
            Photo p = photos.get(0);
            body = p.getTitle();
            //RecordSet r0 = RecordSet.fromJson(attachments);
        } else if (type == Post.POST_LINK) {

            if (attachments.length() > 2) {
                ArrayNode aNode = (ArrayNode) JsonHelper.parse(attachments);
                if ((aNode != null) && (aNode.get(0) != null))
                    body = aNode.get(0).get("url").getTextValue();
            }
        }
        return body;
    }
}
