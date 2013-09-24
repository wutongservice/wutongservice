package com.borqs.server.impl.opline;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.opline.Operation;
import com.borqs.server.platform.feature.opline.Operations;
import com.borqs.server.platform.sql.*;
import com.borqs.server.platform.util.ObjectHolder;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class OpLineDb extends SqlSupport {
    private Table historyTable;

    public OpLineDb() {
    }

    public Table getHistoryTable() {
        return historyTable;
    }

    public void setHistoryTable(Table historyTable) {
        this.historyTable = historyTable;
    }

    private ShardResult sharedHistory(long userId) {
        return historyTable.shard(userId);
    }

    public void puts(final Context ctx, final Operations opers) {
        if (!opers.isSameUser())
            throw new ServerException(E.VARIOUS_OPERATOR);

        long userId = opers.getFirstUserId();
        //if (userId == 0)
        //    throw new ServerException(E.INVALID_USER);

        final ShardResult sr = sharedHistory(userId);
        sqlExecutor.openConnection(sr.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                List<String> sqls = OpLineSql.insertOperations(sr.table, opers);
                SqlExecutor.executeUpdate(ctx, conn, sqls);
                return null;
            }
        });
    }

    public Operations getOperationsBefore(final Context ctx, final long userId, final long beforeOperId, final int count) {
        final Operations opers = new Operations();
        final ShardResult sr = sharedHistory(userId);
        sqlExecutor.openConnection(sr.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = OpLineSql.getOperationBefore(sr.table, userId, beforeOperId, count);
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        OpLineRs.readOperations(rs, opers);
                    }
                });
                return null;
            }
        });
        return opers;
    }

    public Operation getLastOperation(final Context ctx, final long userId, final int[] actions) {
        final ShardResult sr = sharedHistory(userId);
        final ObjectHolder<Operation> r = new ObjectHolder<Operation>(null);
        sqlExecutor.openConnection(sr.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = OpLineSql.getLastOperation(sr.table, userId, actions);
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        r.value = OpLineRs.readOperation(rs);
                    }
                });
                return null;
            }
        });
        return r.value;
    }

    public Operations getOpsWithFlag(final Context ctx, final long userId, final int[] actions, final int flag, final long minTime) {
        final Operations opers = new Operations();
        final ShardResult sr = sharedHistory(userId);
        sqlExecutor.openConnection(sr.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = OpLineSql.getOpsWithFlag(sr.table, userId, actions, flag, minTime);
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        OpLineRs.readOperations(rs, opers);
                    }
                });
                return null;
            }
        });
        return opers;
    }

    public Operations getOpsWithFlagByInterval(final Context ctx, final long userId, final int[] actions, final int flag, final long maxInterval, final long minTime) {
        final Operations opers = new Operations();
        final ShardResult sr = sharedHistory(userId);
        sqlExecutor.openConnection(sr.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = OpLineSql.getOpsWithFlag(sr.table, userId, actions, flag, minTime);
                final Operations allOpers = new Operations();
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        OpLineRs.readOperations(rs, allOpers);
                    }
                });
                if (!allOpers.isEmpty()) {
                    opers.add(allOpers.get(0));
                    if (allOpers.size() > 1) {
                        for (int i = 1; i < allOpers.size(); i++) {
                            Operation op = allOpers.get(i);
                            long interval = op.getTime() - allOpers.get(i - 1).getTime();
                            if (interval < 0)
                                interval = -interval;
                            if (interval <= maxInterval)
                                opers.add(op);
                            else
                                break;
                        }
                    }
                }
                return null;
            }
        });
        return opers;
    }

    public void setFlag(final Context ctx, final int flag, final long... operIds) {
        final ShardResult sr = sharedHistory(ctx.getViewer());
        sqlExecutor.openConnection(sr.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = OpLineSql.setFlag(sr.table, flag, operIds);
                SqlExecutor.executeUpdate(ctx, conn, sql);
                return null;
            }
        });
    }
}
