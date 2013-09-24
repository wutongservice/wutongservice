package com.borqs.server.impl.setting;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.sql.*;
import com.borqs.server.platform.util.ObjectHolder;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class SettingDb extends SqlSupport {
    private Table settingTable;

    public SettingDb() {
    }

    public Table getSettingTable() {
        return settingTable;
    }

    public void setSettingTable(Table settingTable) {
        this.settingTable = settingTable;
    }

    private ShardResult shardSetting(long userId) {
        return settingTable.shard(userId);
    }

    public void sets(final Context ctx, final Map<String, String> setting) {
        final long userId = ctx.getViewer();
        final ShardResult settingSR = shardSetting(userId);
        sqlExecutor.openConnection(settingSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                List<String> sqls = SettingSql.insertSetting(settingSR.table, userId, setting);
                SqlExecutor.executeUpdate(ctx, conn, sqls);
                return null;
            }
        });
    }

    public Map<String, String> gets(final Context ctx, final long userId, final String[] keys) {
        final ShardResult settingSR = shardSetting(userId);
        final ObjectHolder<Map<String, String>> r = new ObjectHolder<Map<String, String>>();
        sqlExecutor.openConnection(settingSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = SettingSql.findSetting(settingSR.table, userId, keys);
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        r.value = SettingRs.read(rs);
                    }
                });
                return null;
            }
        });
        return r.value;
    }

    public Map<String, String> getsByStartsWith(final Context ctx, final long userId, final String keyStartsWith) {
        final ShardResult settingSR = shardSetting(userId);
        final ObjectHolder<Map<String, String>> r = new ObjectHolder<Map<String, String>>();
        sqlExecutor.openConnection(settingSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = SettingSql.findSettingStartsWithKey(settingSR.table, userId, keyStartsWith);
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        r.value = SettingRs.read(rs);
                    }
                });
                return null;
            }
        });
        return r.value;
    }

    public void delete(final Context ctx, final String[] keys) {
        final long userId = ctx.getViewer();
        final ShardResult settingSR = shardSetting(userId);
        sqlExecutor.openConnection(settingSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = SettingSql.deleteSetting(settingSR.table, userId, keys);
                SqlExecutor.executeUpdate(ctx, conn, sql);
                return null;
            }
        });
    }
}
