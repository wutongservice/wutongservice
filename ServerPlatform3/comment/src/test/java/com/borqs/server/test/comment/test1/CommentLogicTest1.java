package com.borqs.server.test.comment.test1;

import com.borqs.server.impl.comment.CommentDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.comment.Comment;
import com.borqs.server.platform.feature.comment.CommentLogic;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ConfigurableTestCase;
import com.borqs.server.platform.test.mock.SteveAndBill;

import java.util.List;

public class CommentLogicTest1 extends ConfigurableTestCase {
    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(CommentDb.class);
    }

    private CommentLogic getCommentLogic() {
        return (CommentLogic) getBean("logic.comment");
    }

    Context ctx = Context.createForViewer(SteveAndBill.STEVE_ID);

    public void testCreateComment() {
        CommentLogic commentLogic = this.getCommentLogic();
        Comment comment = getComment();

        commentLogic.createComment(ctx, comment);

        Comment commentQuery = null;
        List<Comment> comment2 = commentLogic.getCommentsOnTarget(ctx, null, null, comment.getTarget());
        if (comment2.size() > 0)
            commentQuery = comment2.get(0);

        assertEquals(comment.getDevice(), commentQuery.getDevice());
        assertEquals(comment.getTarget(), commentQuery.getTarget());
        assertEquals(comment.getMessage(), commentQuery.getMessage());
        assertEquals(comment.getCommenterId(), commentQuery.getCommenterId());
    }

    private Comment getComment() {
        Comment comment = new Comment();

        comment.setDestroyedTime(0);
        comment.setDevice("device1");
        comment.setMessage("message1");
        comment.setCommenterId(ctx.getViewer());

        Target target = new Target(Target.APK, "234");
        comment.setTarget(target);
        return comment;
    }


    public void testDestroyComments() {
        CommentLogic commentLogic = this.getCommentLogic();
        Comment comment = new Comment();

        comment.setDestroyedTime(0);
        comment.setDevice("device1");
        comment.setMessage("message1");
        comment.setCommenterId(ctx.getViewer());

        Target target = new Target(Target.BOOK, "2342222");
        comment.setTarget(target);

        commentLogic.createComment(ctx, comment);
        
        List<Comment> list = commentLogic.getCommentsOnTarget(ctx, null, null, target);

        long[] longs = new long[list.size()];
        int i = 0;
        for (Comment comment2 : list) {
            longs[i++] = comment2.getCommentId();
        }

        for (long commentId : longs)
            commentLogic.destroyComment(ctx, commentId);
    }

    /*public void testDestroyComment() {
        CommentLogic commentLogic = this.getCommentLogic();
        Target target = new Target(Target.APK, "234");
        List<Comment> list = commentLogic.getCommentsOnTarget(ctx, null, null, target);

        for (Comment comment : list) {
            commentLogic.destroyComment(ctx, comment.getCommentId());
        }
    }*/

    public void testUpdateComment() {
        CommentLogic commentLogic = this.getCommentLogic();

        Target target = new Target(Target.APK, "234");
        List<Comment> list = commentLogic.getCommentsOnTarget(ctx, null, null, target);

        long commentId = 0;
        if (list.size() > 0)
            commentId = list.get(0).getCommentId();

        Comment comment = new Comment();
        comment.setCanLike(false);
        comment.setCommentId(commentId);
        Target target2 = new Target(Target.BOOK, "12333");
        comment.setTarget(target2);
        comment.setMessage("Test2");
        commentLogic.updateComment(ctx, comment);
    }

    public void testCountComment() {
        CommentLogic commentLogic = this.getCommentLogic();
        Target target = new Target(Target.APK, "234");
        commentLogic.getCommentCounts(ctx, target);
    }

    public void testCommentsOnTargets() {
        CommentLogic commentLogic = this.getCommentLogic();
        Context ctx = new Context();
        Target target = new Target(Target.APK, "234");
        Target[] targets = {target};
        Page page = new Page();
        page.count = 100;
        page.page = 0;
        commentLogic.getCommentsOnTarget(ctx, null, page, targets);
    }

    public void testCommentsOnTarget() {
        CommentLogic commentLogic = this.getCommentLogic();
        Context ctx = new Context();
        Target target = new Target(Target.APK, "234");

        Page page = new Page();
        page.count = 100;
        page.page = 0;
        List<Comment> commentList = commentLogic.getCommentsOnTarget(ctx, null, page, target);

    }


    public void testGetComments() {
        CommentLogic commentLogic = this.getCommentLogic();
        Comment comment = getComment();
        Comment comment2 = getComment();
        Comment c1 = commentLogic.createComment(ctx, comment);

        Comment c2 = commentLogic.createComment(ctx, comment2);
        
        long[] longs = new long[]{c1.getCommentId(), c2.getCommentId()};

        List<Comment> commentList = commentLogic.getComments(ctx, null, longs);
    }

    public void testGetUserOnTarget() {
        CommentLogic commentLogic = this.getCommentLogic();
        Target target = new Target(Target.APK, "234");
        Page page = new Page();
        page.count = 100;
        page.page = 0;
        commentLogic.getUsersOnTarget(ctx, target, page);
    }




}
