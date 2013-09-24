package com.borqs.server.impl.ignore;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.sql.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class IgnoreDb extends SqlSupport {
    // table
    private Table ignoreTable;


    public IgnoreDb() {
    }

    public Table getIgnoreTable() {
        return ignoreTable;
    }

    public void setIgnoreTable(Table ignoreTable) {
        this.ignoreTable = ignoreTable;
    }


    private ShardResult shardIgnore(long commentId) {
        return ignoreTable.shard(commentId);
    }

    private ShardResult shardIgnore() {
        return ignoreTable.getShard(0);
    }


    public Boolean ignore(final Context ctx, final int feature, final Target... targets) {
        final ShardResult ignoreSR = shardIgnore();

        // check and ignore the duplicate record
        List<Target> targetList = new ArrayList<Target>();
        for(Target t:targets){
            Target target = getIgnoreExists(ctx,ctx.getViewer(),feature,t);
            targetList.add(target);
        }

        final Target[] ts = new Target[targetList.size()];
        targetList.toArray(ts);

        return sqlExecutor.openConnection(ignoreSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                List<String> sql = IgnoreSql.ignore(ctx, ignoreSR.table, feature, ts);
                long l = SqlExecutor.executeUpdate(ctx, conn, sql);
                return l == ts.length;
            }
        });
    }

    public Boolean unIgnore(final Context ctx, final int feature, final Target... targets) {
        final ShardResult ignoreSR = shardIgnore();
        return sqlExecutor.openConnection(ignoreSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                List sqls = IgnoreSql.unIgnore(ctx, ignoreSR.table, feature, targets);
                long l = SqlExecutor.executeUpdate(ctx, conn, sqls);
                return true;
            }
        });
    }


    public Target[] getIgnore(final Context ctx, final long userId, final int feature) {
        final ShardResult ignoreSR = shardIgnore();

        final List<Target> list = new ArrayList<Target>();
        sqlExecutor.openConnection(ignoreSR.db, new SingleConnectionHandler<List<Target>>() {
            @Override
            protected List<Target> handleConnection(Connection conn) {


                String sql = IgnoreSql.getIgnore(ctx, ignoreSR.table, userId, feature);
                SqlExecutor.executeList(ctx, conn, sql, list, new ResultSetReader<Target>() {
                    @Override
                    public Target read(ResultSet rs, Target reuse) throws SQLException {
                        return IgnoreRs.readIgnore(rs, null);
                    }
                });
                return list;
            }
        });
        Target[] ignores = new Target[list.size()];
        return list.toArray(ignores);
    }

    public Target getIgnoreExists(final Context ctx, final long userId, final int feature, final Target target) {
        final ShardResult ignoreSR = shardIgnore();


        return sqlExecutor.openConnection(ignoreSR.db, new SingleConnectionHandler<Target>() {
            @Override
            protected Target handleConnection(Connection conn) {


                String sql = IgnoreSql.getIgnoreExists(ctx, ignoreSR.table, userId, feature, target);
                long l = SqlExecutor.executeInt(ctx, conn, sql, 0);
                if(l==0)
                    return target;
                else
                    return null;
            }
        });

    }
}

