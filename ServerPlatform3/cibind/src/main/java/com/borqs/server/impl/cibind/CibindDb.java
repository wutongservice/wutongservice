package com.borqs.server.impl.cibind;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.cibind.BindingInfo;
import com.borqs.server.platform.sql.*;
import com.borqs.server.platform.util.CollectionsHelper;
import org.apache.commons.lang.Validate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CibindDb extends SqlSupport {
    private Table cibindTable;

    public CibindDb() {
    }

    public Table getCibindTable() {
        return cibindTable;
    }

    public void setCibindTable(Table cibindTable) {
        if (cibindTable != null)
            Validate.isTrue(cibindTable.getShardCount() == 1);
        this.cibindTable = cibindTable;
    }

    private ShardResult shard() {
        return cibindTable.getShard(0);
    }

    public long whoBinding(final Context ctx, final String info) {
        final ShardResult cibindSR = shard();
        return sqlExecutor.openConnection(cibindSR.db, new SingleConnectionHandler<Long>() {
            @Override
            protected Long handleConnection(Connection conn) {
                String sql = CibindSql.findUserByInfo(cibindSR.table, info);
                return SqlExecutor.executeInt(ctx, conn, sql, 0L);
            }
        });
    }

    public Map<String, Long> whoBinding(final Context ctx, final String... infos) {
        final ShardResult cibindSR = shard();
        return sqlExecutor.openConnection(cibindSR.db, new SingleConnectionHandler<Map<String, Long>>() {
            @Override
            protected Map<String, Long> handleConnection(Connection conn) {
                String sql = CibindSql.findUsersByInfos(cibindSR.table, infos);
                final LinkedHashMap<String, Long> m = new LinkedHashMap<String, Long>();
                for (String info : infos)
                    m.put(info, 0L);
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        while (rs.next()) {
                            m.put(rs.getString("info"), rs.getLong("user"));
                        }
                    }
                });
                return m;
            }
        });
    }

    public void bind(final Context ctx, final BindingInfo bi) {
        final ShardResult cibindSR = shard();
        sqlExecutor.openConnection(cibindSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = CibindSql.insertBindingInfo(cibindSR.table, ctx.getViewer(), bi);
                try {
                    SqlExecutor.executeUpdate(ctx, conn, sql);
                } catch (Exception e) {
                    throw new ServerException(E.BINDING_EXISTS, "The binding is exists");
                }
                return null;
            }
        });
    }

    public boolean unbind(final Context ctx, final String info) {
        final ShardResult cibindSR = shard();
        return sqlExecutor.openConnection(cibindSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = CibindSql.findUserByInfo(cibindSR.table, info);
                long userId = SqlExecutor.executeInt(ctx, conn, sql, 0L);
                if (userId > 0 && ctx.getViewer() != userId)
                    return false;

                sql = CibindSql.deleteInfo(cibindSR.table, info);
                return SqlExecutor.executeUpdate(ctx, conn, sql) > 0;
            }
        });
    }

    public Map<Long, BindingInfo[]> getBindingInfo(final Context ctx, final long[] userIds) {
        final ShardResult cibindSR = shard();
        return sqlExecutor.openConnection(cibindSR.db, new SingleConnectionHandler<Map<Long, BindingInfo[]>>() {
            @Override
            protected Map<Long, BindingInfo[]> handleConnection(Connection conn) {
                String sql = CibindSql.getBindingInfo(cibindSR.table, userIds);
                final HashMap<Long, List<BindingInfo>> m = new HashMap<Long, List<BindingInfo>>();
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        CibindRs.readMultipleBindingInfo(rs, m);
                    }
                });
                return CollectionsHelper.listMapToArrayMap(m, BindingInfo.class);
            }
        });
    }
}
