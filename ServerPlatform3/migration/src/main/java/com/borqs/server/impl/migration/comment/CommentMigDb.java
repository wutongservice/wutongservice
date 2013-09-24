package com.borqs.server.impl.migration.comment;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.comment.Comment;
import com.borqs.server.platform.feature.comment.Comments;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.*;
import com.borqs.server.platform.util.CollectionsHelper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;

public class CommentMigDb extends SqlSupport {
    private static final Logger L = Logger.get(CommentMigDb.class);

    private Map<Long, String> userIdMap;
    // table
    private Table commentTable;

    public CommentMigDb() {
    }

    public void setUserIdMap(Map<Long, String> userIdMap) {
        this.userIdMap = userIdMap;
    }

    public Table getCommentTable() {
        return commentTable; 
    }

    public void setCommentTable(Table commentTable) {
        this.commentTable = commentTable;
    }

    private ShardResult shardComment() {
        return commentTable.getShard(0);
    }

    public Comments getComments(final Context ctx) {
        final ShardResult commentSR = shardComment();

        return sqlExecutor.openConnection(commentSR.db, new SingleConnectionHandler<Comments>() {
            @Override
            protected Comments handleConnection(Connection conn) {

                final Comments postList = new Comments();
                String sql = CommentMigSql.getComment(commentSR.table);
                SqlExecutor.executeList(ctx, conn, sql, postList, new ResultSetReader<Comment>() {
                    @Override
                    public Comment read(ResultSet rs, Comment reuse) throws SQLException {
                        return CommentMigRs.readComment(rs, null, userIdMap);
                    }
                });
                return postList;
            }
        });
    }

    public long[] getAllCommentIds(final Context ctx) {
        final ShardResult commentSR = shardComment();
        return sqlExecutor.openConnection(commentSR.db, new SingleConnectionHandler<long[]>() {
            @Override
            protected long[] handleConnection(Connection conn) {
                String sql = CommentMigSql.findAllCommentIds(commentSR.table);
                final HashSet<Long> existsIds = new HashSet<Long>();
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        CommentMigRs.readIds(rs, existsIds);
                    }
                });
                return CollectionsHelper.toLongArray(existsIds);
            }
        });
    }

}

