package com.borqs.server.impl.migration.comment;


import com.borqs.server.impl.comment.CommentDb;
import com.borqs.server.impl.migration.CMDRunner;
import com.borqs.server.impl.migration.account.AccountMigImpl;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.comment.Comment;
import com.borqs.server.platform.feature.comment.Comments;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;

import java.util.*;

public class CommentMigImpl implements CMDRunner {

    private static final Logger L = Logger.get(CommentMigImpl.class);

    private final CommentMigDb db_migration = new CommentMigDb();
    private final CommentDb dbNewComment = new CommentDb();

    private AccountMigImpl account;

    public void setAccount(AccountMigImpl account) {
        this.account = account;
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        dbNewComment.setSqlExecutor(sqlExecutor);
        db_migration.setSqlExecutor(sqlExecutor);
    }

    public void setNewCommentTable(Table newCommentTable) {
        dbNewComment.setCommentTable(newCommentTable);
    }

    public void setNewCommentTargetTable(Table newCommentTargetTable) {
        dbNewComment.setCommentTargetTable(newCommentTargetTable);
    }

    public void setOldCommentTable(Table oldCommentTable) {
        db_migration.setCommentTable(oldCommentTable);
    }

    @Override
    public List<String> getDependencies() {
        List<String> list = new ArrayList<String>();
        list.add("account.mig");
        list.add("stream.mig");
        return list;
    }

    @Override
    public void run(String cmd, Properties config) {
        if (cmd.equals("comment.mig")) {
            commentMigration(Context.create());
        }
    }

    public void commentMigration(Context ctx) {

        final LogCall LC = LogCall.startCall(L, CommentMigImpl.class, "commentMigration", ctx);

        Comments comments = null;

        //check comment
        List<Long> postIds = new ArrayList<Long>();
        try {

            db_migration.setUserIdMap(getAllUserIdMap(ctx));

            comments = db_migration.getComments(ctx);

            for (Comment comment : comments) {
                try {
                    if (comment != null) {
                        dbNewComment.createComment(ctx, comment);
                        postIds.add(comment.getCommenterId());
                    }
                } catch (RuntimeException e) {
                    LC.endCall();
                    throw e;
                }
            }
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall();
            throw e;
        }
    }

    public Map<Long, String> getAllCommentIdMap(Context ctx) {
        long[] list = db_migration.getAllCommentIds(ctx);
        Map<Long, String> map = new HashMap<Long, String>();
        for (Long l : list) {
            map.put(l, String.valueOf(l));
        }
        return map;
    }

    private Map<Long, String> getAllUserIdMap(Context ctx) {
        return account.getAllUserIdMap(ctx);
    }

}
