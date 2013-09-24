package com.borqs.server.impl.migration.request;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.request.Request;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RequestMigDb extends SqlSupport {
    private static final Logger L = Logger.get(RequestMigDb.class);

    private Map<Long, String> userIdMap;

    // table
    private Table requestTable;

    public RequestMigDb() {
    }

    public void setUserIdMap(Map<Long, String> userIdMap) {
        this.userIdMap = userIdMap;
    }



    public Table getRequestTable() {
        return requestTable;
    }

    public void setRequestTable(Table requestTable) {
        this.requestTable = requestTable;
    }

    private ShardResult shardRequest() {
        return requestTable.getShard(0);
    }

    public List<Request> getRequest(final Context ctx) {
        final ShardResult settingSR = shardRequest();

        return sqlExecutor.openConnection(settingSR.db, new SingleConnectionHandler<List<Request>>() {
            @Override
            protected List<Request> handleConnection(Connection conn) {

                final List<Request> settingList = new ArrayList<Request>();
                String sql = RequestMigSql.getRequests(ctx, settingSR.table);
                SqlExecutor.executeList(ctx, conn, sql, settingList, new ResultSetReader<Request>() {
                    @Override
                    public Request read(ResultSet rs, Request reuse) throws SQLException {
                        return RequestMigRs.readRequest(rs, null, userIdMap);
                    }
                });
                return settingList;
            }
        });
    }

}

