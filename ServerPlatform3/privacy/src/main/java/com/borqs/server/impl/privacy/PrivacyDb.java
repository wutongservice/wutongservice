package com.borqs.server.impl.privacy;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.privacy.PrivacyEntry;
import com.borqs.server.platform.sql.*;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.ObjectHolder;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PrivacyDb extends SqlSupport {
    private Table privacyTable;

    public PrivacyDb() {
    }

    public Table getPrivacyTable() {
        return privacyTable;
    }

    public void setPrivacyTable(Table privacyTable) {
        this.privacyTable = privacyTable;
    }

    private ShardResult shardPrivacy(long userId) {
        return privacyTable.shard(userId);
    }

    public void sets(final Context ctx, final PrivacyEntry... entries) {
        final long userId = ctx.getViewer();
        final ShardResult privacySR = shardPrivacy(userId);
        sqlExecutor.openConnection(privacySR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                List<String> sqls = PrivacySql.insertPrivacy(privacySR.table, userId, entries);
                SqlExecutor.executeUpdate(ctx, conn, sqls);
                return null;
            }
        });
    }

    public List<PrivacyEntry> gets(final Context ctx, final long userId, final String[] res) {
        final ShardResult privacySR = shardPrivacy(userId);
        final ObjectHolder<List<PrivacyEntry>> r = new ObjectHolder<List<PrivacyEntry>>();
        sqlExecutor.openConnection(privacySR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = PrivacySql.findPrivacy(privacySR.table, userId, res);
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        r.value = PrivacyRs.read(rs);
                    }
                });
                return null;
            }
        });
        return r.value;
    }

    public List<PrivacyEntry> check(final Context ctx, final long[] userIds, final String res, final int scope, final String id) {
        final List<PrivacyEntry> list = new ArrayList<PrivacyEntry>();
        final GroupedShardResults groupedPropSR = TableHelper.shard(privacyTable, userIds);

        for (final ShardResult privacySR : groupedPropSR.getShardResults()) {
            final long[] userIdsInShard = CollectionsHelper.toLongArray(groupedPropSR.get(privacySR));
            sqlExecutor.openConnection(privacySR.db, new SingleConnectionHandler<Object>() {
                @Override
                protected Object handleConnection(Connection conn) {
                    String sql = PrivacySql.check(privacySR.table, userIdsInShard, res, scope, id);
                    SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                        @Override
                        public void handle(ResultSet rs) throws SQLException {
                            list.addAll(PrivacyRs.read(rs));
                        }
                    });
                    return null;
                }
            });
        }

        return list;
    }

    public void delete(final Context ctx, final String[] resources) {
        final long userId = ctx.getViewer();
        final ShardResult privacySR = shardPrivacy(userId);
        sqlExecutor.openConnection(privacySR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = PrivacySql.deletePrivacy(privacySR.table, userId, resources);
                SqlExecutor.executeUpdate(ctx, conn, sql);
                return null;
            }
        });
    }
}
