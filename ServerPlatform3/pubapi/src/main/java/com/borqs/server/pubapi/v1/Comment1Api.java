package com.borqs.server.pubapi.v1;

import com.borqs.server.compatible.CompatibleComment;
import com.borqs.server.compatible.CompatibleTarget;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.comment.Comment;
import com.borqs.server.platform.feature.comment.CommentLogic;
import com.borqs.server.platform.feature.comment.Comments;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.mq.ContextObject;
import com.borqs.server.platform.mq.QueueName;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.RandomHelper;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.PublicApiSupport;

@IgnoreDocument
public class Comment1Api extends PublicApiSupport {
    private AccountLogic account;
    private CommentLogic comment;
    private QueueName commentQueue;

    public Comment1Api() {
    }

    public QueueName getCommentQueue() {
        return commentQueue;
    }

    public void setCommentQueue(QueueName commentQueue) {
        this.commentQueue = commentQueue;
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public CommentLogic getComment() {
        return comment;
    }

    public void setComment(CommentLogic comment) {
        this.comment = comment;
    }

    private static Target getCommentTarget(Request req) {
        return new Target(CompatibleTarget.v1ToV2Type(req.getInt("object", CompatibleTarget.V1_POST)), req.checkString("target"));
    }

    @Route(url = "/comment/create")
    public void createComment(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());

        String message = req.checkString("message");
        PeopleIds addTo = PeopleIds.parseAddTo(message);

        long now = DateHelper.nowMillis();
        Comment comment0 = new Comment(RandomHelper.generateId(now));
        comment0.setCreatedTime(now);
        comment0.setCommenterId(ctx.getViewer());
        comment0.setDevice(ctx.getRawUserAgent());
        comment0.setTarget(getCommentTarget(req));
        comment0.setCanLike(req.getBoolean("can_like", true));
        comment0.setMessage(message);
        comment0.setAddTo(addTo);

        String[] v1Cols = CompatibleComment.V1_FULL_COLUMNS;
        new ContextObject(ctx, ContextObject.TYPE_CREATE, comment0).sendThisWith(commentQueue);
        comment0 = comment.expand(ctx, CompatibleComment.v1ToV2Columns(v1Cols), comment0);
        resp.body(RawText.of(CompatibleComment.commentToJson(comment0, v1Cols, true)));
    }

    @Route(url = "/comment/destroy")
    public void destroyComments(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());

        long[] commentIds = req.checkLongArray("comments", ",");
        for (long commentId : commentIds) {
            comment.destroyComment(ctx, commentId);
        }
        resp.body(true);
    }

    @Route(url = "/comment/count")
    public void getCommentCount(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        long n = comment.getCommentCount(ctx, getCommentTarget(req));
        resp.body(n);
    }

    @Route(url = "/comment/get")
    public void getComments(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        String[] v1Cols = CompatibleComment.expandV1Columns(req.getStringArray("columns", ",", CompatibleComment.V1_FULL_COLUMNS));
        long[] commentIds = req.checkLongArray("comments", ",");
        Comments comments = comment.getComments(ctx, CompatibleComment.v1ToV2Columns(v1Cols), commentIds);
        resp.body(RawText.of(CompatibleComment.commentsToJson(comments, v1Cols, true)));
    }

    @Route(url = "/comment/for")
    public void getCommentsFor(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        Target commentTarget = getCommentTarget(req);
        String[] v1Cols = CompatibleComment.V1_FULL_COLUMNS;
        Page page = req.getPage(20, 100);

        Comments comments = comment.getCommentsOnTarget(ctx, CompatibleComment.v1ToV2Columns(v1Cols), page, commentTarget);
        resp.body(RawText.of(CompatibleComment.commentsToJson(comments, v1Cols, true)));
    }

    @Route(url = "/comment/can_like")
    public void getCommentCanLike(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        Comment comment0 = comment.getComment(ctx, new String[]{Comment.COL_COMMENT_ID, Comment.COL_CAN_LIKE}, req.checkLong("comment"));
        resp.body(comment0 != null && comment0.getCanLike());
    }
}
