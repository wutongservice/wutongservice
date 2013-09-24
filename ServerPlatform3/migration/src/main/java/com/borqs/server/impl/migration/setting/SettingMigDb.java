package com.borqs.server.impl.migration.setting;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettingMigDb extends SqlSupport {
    private static final Logger L = Logger.get(SettingMigDb.class);

    private Map<Long, String> userIdMap;

    // table
    private Table settingTable;

    public SettingMigDb() {
    }

    public void setUserIdMap(Map<Long, String> userIdMap) {
        this.userIdMap = userIdMap;
    }



    public Table getSettingTable() {
        return settingTable;
    }

    public void setSettingTable(Table settingTable) {
        this.settingTable = settingTable;
    }

    private ShardResult shardSetting() {
        return settingTable.getShard(0);
    }

    public List<MigrationSetting> getSetting(final Context ctx) {
        final ShardResult settingSR = shardSetting();

        return sqlExecutor.openConnection(settingSR.db, new SingleConnectionHandler<List<MigrationSetting>>() {
            @Override
            protected List<MigrationSetting> handleConnection(Connection conn) {

                final List<MigrationSetting> settingList = new ArrayList<MigrationSetting>();
                String sql = SettingMigSql.getSettings(ctx, settingSR.table);
                SqlExecutor.executeList(ctx, conn, sql, settingList, new ResultSetReader<MigrationSetting>() {
                    @Override
                    public MigrationSetting read(ResultSet rs, MigrationSetting reuse) throws SQLException {
                        return SettingMigRs.readSetting(rs, null, userIdMap);
                    }
                });
                return settingList;
            }
        });
    }

}

