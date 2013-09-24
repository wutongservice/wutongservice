package com.borqs.server.impl.migration.suggest;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.psuggest.PeopleSuggest;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SuggestMigDb extends SqlSupport {
    private static final Logger L = Logger.get(SuggestMigDb.class);

    private Map<Long, String> userIdMap;

    // table
    private Table suggestTable;

    public SuggestMigDb() {
    }

    public void setUserIdMap(Map<Long, String> userIdMap) {
        this.userIdMap = userIdMap;
    }



    public Table getSuggestTable() {
        return suggestTable;
    }

    public void setSuggestTable(Table suggestTable) {
        this.suggestTable = suggestTable;
    }

    private ShardResult shardSetting() {
        return suggestTable.getShard(0);
    }

    public List<PeopleSuggest> getSuggest(final Context ctx) {
        final ShardResult suggestSR = shardSetting();

        return sqlExecutor.openConnection(suggestSR.db, new SingleConnectionHandler<List<PeopleSuggest>>() {
            @Override
            protected List<PeopleSuggest> handleConnection(Connection conn) {

                final List<PeopleSuggest> settingList = new ArrayList<PeopleSuggest>();
                String sql = SuggestMigSql.getSuggest(ctx, suggestSR.table);
                SqlExecutor.executeList(ctx, conn, sql, settingList, new ResultSetReader<PeopleSuggest>() {
                    @Override
                    public PeopleSuggest read(ResultSet rs, PeopleSuggest reuse) throws SQLException {
                        return SuggestMigRs.readSuggest(rs, null, userIdMap);
                    }
                });
                return settingList;
            }
        });
    }

}

