package com.broqs.server.impl.configration;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.configuration.Config;
import com.borqs.server.platform.sql.*;
import org.apache.commons.lang.Validate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ConfigDb extends SqlSupport {
    private Table configTable;

    public ConfigDb() {
    }

    public Table getConfigTable() {
        return configTable;
    }

    public void setConfigTable(Table configTable) {
        if (configTable != null)
            Validate.isTrue(configTable.getShardCount() == 1);
        this.configTable = configTable;
    }

    private ShardResult shard() {
        return configTable.getShard(0);
    }


    public long saveConfig(final Context ctx, final Config Config) {
        final ShardResult configSR = shard();
        return sqlExecutor.openConnection(configSR.db, new SingleConnectionHandler<Long>() {
            @Override
            protected Long handleConnection(Connection conn) {
                String sql = ConfigSql.insertConfig(configSR.table, Config);
                return SqlExecutor.executeUpdate(ctx, conn, sql);
            }
        });
    }

    public List<Config> getConfigs(final Context ctx, final long userid, final String key, final int version) {

        final List<Config> configList = new ArrayList<Config>();
        final ShardResult configSR = shard();

        return sqlExecutor.openConnection(configSR.db, new SingleConnectionHandler<List<Config>>() {
            @Override
            protected List<Config> handleConnection(Connection conn) {
                String sql = ConfigSql.getConfigs(configSR.table, userid, key, version);
                SqlExecutor.executeList(ctx, conn, sql, configList, new ResultSetReader<Config>() {
                    @Override
                    public Config read(ResultSet rs, Config reuse) throws SQLException {
                        return ConfigRs.readConfig(rs, null);
                    }
                });
                return configList;
            }
        });
    }

    public List<Config> getConfigByUserIds(final Context ctx, final long userId) {

        final List<Config> configList = new ArrayList<Config>();
        final ShardResult configSR = shard();

        return sqlExecutor.openConnection(configSR.db, new SingleConnectionHandler<List<Config>>() {
            @Override
            protected List<Config> handleConnection(Connection conn) {
                String sql = ConfigSql.getConfigByUserId(configSR.table, userId);
                SqlExecutor.executeList(ctx, conn, sql, configList, new ResultSetReader<Config>() {
                    @Override
                    public Config read(ResultSet rs, Config reuse) throws SQLException {
                        return ConfigRs.readConfig(rs, null);
                    }
                });
                return configList;
            }
        });
    }

    public boolean deleteConfig(final Context ctx, final long userId, final String key, final int version_code) {
        final ShardResult configSR = shard();
        return sqlExecutor.openConnection(configSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = ConfigSql.deleteConfig(configSR.table, userId, key, version_code);
                long n = SqlExecutor.executeUpdate(ctx, conn, sql);
                return n == 1;
            }
        });
    }
}
