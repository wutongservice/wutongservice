package com.borqs.server.impl.comment;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.comment.Comment;
import com.borqs.server.platform.feature.comment.Comments;
import com.borqs.server.platform.sql.*;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.DateHelper;
import org.apache.commons.lang.ArrayUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class CommentDb extends SqlSupport {
    // table
    private Table commentTable;
    private Table commentTargetTable;

    public CommentDb() {
    }

    public Table getCommentTable() {
        return commentTable;
    }

    public void setCommentTable(Table commentTable) {
        this.commentTable = commentTable;
    }

    public Table getCommentTargetTable() {
        return commentTargetTable;
    }

    public void setCommentTargetTable(Table commentTargetTable) {
        this.commentTargetTable = commentTargetTable;
    }

    private ShardResult shardCommentTarget() {
        return commentTargetTable.getShard(0);
    }

    private ShardResult shardComment(long commentId) {
        return commentTable.shard(commentId);
    }

    private ShardResult shardComment() {
        return commentTable.getShard(0);
    }

    private GroupedShardResults shardComment(String... ids) {
        return TableHelper.shard(commentTable, ids);
    }

    private GroupedShardResults shardComment(long... ids) {
        return TableHelper.shard(commentTable, ids);
    }

    private GroupedShardResults shardCommentTarget(String... ids) {
        return TableHelper.shard(commentTargetTable, ids);
    }

    public Comment createComment(final Context ctx, final Comment comment) {
        final ShardResult commentSR = shardComment();
        final ShardResult commentTargetSR = shardCommentTarget();
        return sqlExecutor.openConnection(commentSR.db, new SingleConnectionHandler<Comment>() {
            @Override
            protected Comment handleConnection(Connection conn) {
                //comment.setCommentId(RandomHelper.generateId());
                String sql = CommentSql.saveComment(commentSR.table, comment);
                SqlExecutor.executeUpdate(ctx, conn, sql);

                String sqlTarget = CommentSql.saveCommentTarget(commentTargetSR.table, comment);
                SqlExecutor.executeUpdate(ctx, conn, sqlTarget);

                return comment;
            }
        });
    }

    public boolean destroyComments(final Context ctx, final long... commentIds) {
        if (ArrayUtils.isEmpty(commentIds))
            return true;

        final long now = DateHelper.nowMillis();
        final ShardResult commentSR = shardComment();
        return sqlExecutor.openConnection(commentSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = CommentSql.disableComments(ctx, commentSR.table, now, commentIds);
                long n = SqlExecutor.executeUpdate(ctx, conn, sql);
                return n > 0;
            }
        });
    }

    public boolean updateComment(final Context ctx, final Comment comment) {
        final ShardResult commentSR = comment.getMessage() != null ? shardComment() : null;
        final ShardResult commentTargetSR = comment.getTarget().id != null ? shardCommentTarget() : null;

        String[] dbs = ShardResult.getDbs(commentSR, commentTargetSR);

        if (dbs[0] != null || dbs[1] != null) {
            return sqlExecutor.openConnections(dbs, new ConnectionsHandler<Boolean>() {
                @Override
                public Boolean handle(Connection[] conns) {
                    Connection commentConn = conns[0];
                    Connection commentTargetConn = conns[1];
                    long isUpdate = 0;
                    if (commentConn != null) {
                        String updateCommentSql = CommentSql.updateComment(commentSR.table, comment);
                        isUpdate = SqlExecutor.executeUpdate(ctx, commentConn, updateCommentSql);
                    }
                    if (commentTargetConn != null && isUpdate != 0) {
                        String updateTargetSqls = CommentSql.updateCommentTarget(commentTargetSR.table, comment);
                        SqlExecutor.executeUpdate(ctx, commentTargetConn, updateTargetSqls);
                    }
                    return true;
                }
            });
        } else {
            return false;
        }
    }


    public Map<Target, Integer> getCommentCounts(final Context ctx, final Target... targets) {
        String[] strings = new String[targets.length];
        int i = 0;
        for (Target target : targets) {
            strings[i++] = target.id;
        }
        final Map<Target, Integer> map = new HashMap<Target, Integer>();
        GroupedShardResults groupedPropSR = shardCommentTarget(strings);

        for (final ShardResult propSR : groupedPropSR.getShardResults()) {
            //final long[] commentIdsInShard = CollectionsHelper.toLongArray(groupedPropSR.get(propSR));
            sqlExecutor.openConnection(propSR.db, new SingleConnectionHandler<Map<Target, Integer>>() {
                @Override
                protected Map<Target, Integer> handleConnection(Connection conn) {
                    for (Target target : targets) {
                        String sql = CommentSql.getCommentCount(propSR.table, target);
                        map.put(target, (int) SqlExecutor.executeInt(ctx, conn, sql, 0));
                    }
                    return map;
                }
            });
        }
        return map;
    }

    public Map<Target, long[]> getCommentsOnTarget(final Context ctx, final Page page, final Target... targets) {
        String[] strings = new String[targets.length];
        int i = 0;
        for (Target target : targets) {
            strings[i++] = target.id;
        }

        final Map<Target, long[]> map = new HashMap<Target, long[]>();
        GroupedShardResults groupedPropSR = shardComment(strings);

        for (final ShardResult propSR : groupedPropSR.getShardResults()) {
            sqlExecutor.openConnection(propSR.db, new SingleConnectionHandler<Map<Target, long[]>>() {
                @Override
                protected Map<Target, long[]> handleConnection(Connection conn) {
                    for (Target target : targets) {
                        final HashSet<Long> existsIds = new HashSet<Long>();
                        String sql = CommentSql.getCommentsOnTarget(propSR.table, target, page);
                        SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                            @Override
                            public void handle(ResultSet rs) throws SQLException {
                                CommentRs.readIds(rs, existsIds);
                            }
                        });
                        long[] ids = CollectionsHelper.toLongArray(existsIds);
                        map.put(target, ids);
                    }
                    return map;
                }
            });
        }
        return map;
    }


    public Comments getComments(final Context ctx, String[] expCols, final long... commentIds) {

        final Comments commentList = new Comments();

        GroupedShardResults groupedPropSR = shardComment(commentIds);
        for (final ShardResult propSR : groupedPropSR.getShardResults()) {
            sqlExecutor.openConnection(propSR.db, new SingleConnectionHandler<List<Comment>>() {
                @Override
                protected List<Comment> handleConnection(Connection conn) {
                    String sql = CommentSql.getComments(propSR.table, commentIds);
                    SqlExecutor.executeList(ctx, conn, sql, commentList, new ResultSetReader<Comment>() {
                        @Override
                        public Comment read(ResultSet rs, Comment reuse) throws SQLException {
                            return CommentRs.readComment(rs, null);
                        }
                    });
                    return commentList;
                }
            });
        }
        return commentList;
    }

    public String[] getTargetIdsOrderByCommentCount(final Context ctx, final int targetType, final boolean asc, final Page page) {
        if (commentTargetTable.getShardCount() != 1)
            throw new ServerException(E.DATA, "Can't support sharding table");

        final ShardResult targetIndexSR = shardCommentTarget();
        final ArrayList<String> targetIds = new ArrayList<String>();
        sqlExecutor.openConnection(targetIndexSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = CommentSql.getTargetIdsOrderByCommentCount(targetIndexSR.table, targetType, asc, page);
                SqlExecutor.executeList(ctx, conn, sql, targetIds, new ResultSetReader<String>() {
                    @Override
                    public String read(ResultSet rs, String reuse) throws SQLException {
                        return rs.getString("target_id");
                    }
                });
                return null;
            }
        });
        return  targetIds.toArray(new String[targetIds.size()]);
    }
}

