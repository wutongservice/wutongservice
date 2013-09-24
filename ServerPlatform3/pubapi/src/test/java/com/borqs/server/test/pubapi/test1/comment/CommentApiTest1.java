package com.borqs.server.test.pubapi.test1.comment;

import com.borqs.server.impl.comment.CommentDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.comment.Comment;
import com.borqs.server.platform.feature.comment.CommentLogic;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ServletTestCase;
import com.borqs.server.platform.test.TestApp;
import com.borqs.server.platform.test.TestComment;
import com.borqs.server.platform.test.TestHttpApiClient;
import com.borqs.server.platform.test.mock.SteveAndBill;
import com.borqs.server.platform.web.AbstractHttpClient;

import java.text.NumberFormat;
import java.util.List;

public class CommentApiTest1 extends ServletTestCase {
    public static final String PUB_API = "servlet.pubApi";

    @Override
    protected String[] getServletBeanIds() {
        return new String[]{PUB_API};
    }

    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(CommentDb.class);
    }

    private CommentLogic getCommentLogic() {
        return (CommentLogic) getBean("logic.comment");
    }

    Context ctx = Context.createForViewer(SteveAndBill.STEVE_ID);

    public void testCommentCreate() {
        List<Comment> list = new TestComment().createComments(ctx, null, 100);

        Comment comment = null;
        if (list.size() > 0) {
            NumberFormat n = NumberFormat.getInstance();
            n.setMaximumFractionDigits(1);
            int i = list.size();
            int b = (int) (Math.random() * i) % i;
            comment = list.get(b);
        }
        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, SteveAndBill.steveTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/comment/create", new Object[][]{
                {"message", comment.getMessage()},
                {"commenter", comment.getCommenterId()},
                {"device", comment.getDevice()},
                {"target_type", comment.getTarget().type},
                {"target_id", comment.getTarget().id},
                {"can_like", comment.getCanLike()}
        });

        CommentLogic commentLogic = getCommentLogic();
        Comment commentDB = commentLogic.getCommentsOnTarget(ctx, null, null, comment.getTarget()).get(0);

        assertEquals(comment.getMessage(), commentDB.getMessage());
        assertEquals(comment.getTarget().type, commentDB.getTarget().type);
        assertEquals(comment.getTarget().id, commentDB.getTarget().id);
        assertEquals(comment.getCanLike(), commentDB.getCanLike());
        assertEquals(comment.getCommenterId(), commentDB.getCommenterId());
        assertEquals(comment.getDevice(), commentDB.getDevice());


    }

    public void testCommentDestroy() {

        Target target = new Target(Target.APK, "234");
        CommentLogic commentLogic = getCommentLogic();

        List<Comment> list = commentLogic.getCommentsOnTarget(ctx, null, null, target);
        if (list.size() < 1) {
            Comment comment = new TestComment().createComment(ctx, null);
            commentLogic.createComment(ctx, comment);
            list = commentLogic.getCommentsOnTarget(ctx, null, null, comment.getTarget());
        }

        StringBuffer sb = new StringBuffer();
        for (Comment comment : list) {
            sb.append(comment.getCommentId()).append(",");
        }
        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, SteveAndBill.steveTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/comment/destroy", new Object[][]{
                {"comments", sb.substring(0, sb.length() - 1)}
        });
    }

    public void testCommentsOnTarget() {
        CommentLogic commentLogic = getCommentLogic();
        Comment comment1 = new TestComment().createComment(ctx, null);
        commentLogic.createComment(ctx, comment1);

        List<Comment> list = commentLogic.getCommentsOnTarget(ctx, null, null, comment1.getTarget());
        Comment comment = list.get(0);

        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, SteveAndBill.steveTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/comment/target", new Object[][]{
                {"target_type", comment.getTarget().type},
                {"target_id", comment.getTarget().id}
        });
    }

    public void testComentCount() {
        CommentLogic commentLogic = getCommentLogic();
        Comment comment1 = new TestComment().createComment(ctx, null);
        commentLogic.createComment(ctx, comment1);

        List<Comment> list = commentLogic.getCommentsOnTarget(ctx, null, null, comment1.getTarget());
        Comment comment = list.get(0);

        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, SteveAndBill.steveTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/comment/count", new Object[][]{
                {"target_type", comment.getTarget().type},
                {"target_id", comment.getTarget().id}
        });
    }

    public void testCommentGet() {
        Target target = new Target(Target.APK, "234");
        CommentLogic commentLogic = getCommentLogic();

        List<Comment> list = commentLogic.getCommentsOnTarget(ctx, null, null, target);
        if (list.size() < 1) {
            Comment comment = new TestComment().createComment(ctx, null);
            commentLogic.createComment(ctx, comment);
            list = commentLogic.getCommentsOnTarget(ctx, null, null, comment.getTarget());
        }

        StringBuffer sb = new StringBuffer();
        for (Comment comment : list) {
            sb.append(comment.getCommentId()).append(",");
        }

        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, SteveAndBill.steveTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/comment/get", new Object[][]{
                {"comments", sb.substring(0, sb.length() - 1)}
        });
    }

    public void testCanLike() {
        CommentLogic commentLogic = getCommentLogic();
        Comment comment1 = new TestComment().createComment(ctx, null);
        commentLogic.createComment(ctx, comment1);

        List<Comment> list = commentLogic.getCommentsOnTarget(ctx, null, null, comment1.getTarget());
        Comment comment = list.get(0);


        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, SteveAndBill.steveTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/comment/can_like", new Object[][]{
                {"comment", comment.getCommentId()}
        });
    }

    public void testUpdateLike() {
        CommentLogic commentLogic = getCommentLogic();
        Comment comment1 = new TestComment().createComment(ctx, null);
        commentLogic.createComment(ctx, comment1);

        List<Comment> list = commentLogic.getCommentsOnTarget(ctx, null, null, comment1.getTarget());
        Comment comment = list.get(0);


        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, SteveAndBill.steveTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/comment/updateComment", new Object[][]{
                {"comment", comment.getCommentId()}
        });
    }

}
