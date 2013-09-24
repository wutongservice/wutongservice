package com.borqs.server.impl.migration.circle;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CircleMigDb extends SqlSupport {
    private static final Logger L = Logger.get(CircleMigDb.class);
    // table

    private Table circleTable;

    private Table circleOldTable;

    public CircleMigDb() {
    }


    public Table getCircleTable() {
        return circleTable;
    }

    public void setCircleTable(Table circleTable) {
        this.circleTable = circleTable;
    }

    public Table getCircleOldTable() {
        return circleOldTable;
    }

    public void setCircleOldTable(Table circleOldTable) {
        this.circleOldTable = circleOldTable;
    }

    private ShardResult shardCircle() {
        return circleTable.getShard(0);
    }

    private ShardResult shardOldCircle() {
        return circleOldTable.getShard(0);
    }
    
    public void circleMigration(final Context ctx) {
        final ShardResult circleSR = shardCircle();
        final ShardResult circleOldSR = shardOldCircle();

        final List<MigrationCircle> circleList = new ArrayList<MigrationCircle>();

        sqlExecutor.openConnection(circleOldSR.db, new SingleConnectionHandler<List<MigrationCircle>>() {
            @Override
            protected List<MigrationCircle> handleConnection(Connection conn) {


                String sql = CircleMigSql.getCircles( circleOldSR.table);
                SqlExecutor.executeList(ctx, conn, sql, circleList, new ResultSetReader<MigrationCircle>() {
                    @Override
                    public MigrationCircle read(ResultSet rs, MigrationCircle reuse) throws SQLException {
                        return CircleMigRs.readCircle(rs, null);
                    }
                });
                return circleList;
            }
        });

        if (circleList.size() > 0)
            sqlExecutor.openConnection(circleSR.db, new SingleConnectionHandler<Boolean>() {
                @Override
                protected Boolean handleConnection(Connection conn) {
                    List<String> sqls = CircleMigSql.insertCircles(circleSR.table, circleList);
                    long commentId = SqlExecutor.executeUpdate(ctx, conn, sqls);

                    return true;
                }
            });
    }

}

