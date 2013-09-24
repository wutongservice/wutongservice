package com.broqs.server.impl.staticfile;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.staticfile.video.StaticFile;
import com.borqs.server.platform.sql.*;
import org.apache.commons.lang.Validate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StaticFileDb extends SqlSupport {
    private Table staticFileTable;

    public StaticFileDb() {
    }



    public void setStaticFileTable(Table staticFile) {
        if (staticFile != null)
            Validate.isTrue(staticFile.getShardCount() == 1);
        this.staticFileTable = staticFile;
    }

    private ShardResult shard() {
        return staticFileTable.getShard(0);
    }


    public long saveStaticFile(final Context ctx, final StaticFile staticFile) {
        final ShardResult staticFileSR = shard();
        return sqlExecutor.openConnection(staticFileSR.db, new SingleConnectionHandler<Long>() {
            @Override
            protected Long handleConnection(Connection conn) {
                String sql = StaticFileSql.insertStaticFile(staticFileSR.table, staticFile);
                return SqlExecutor.executeUpdate(ctx, conn, sql);
            }
        });
    }

    public List<StaticFile> getStaticFiles(final Context ctx, final long... staticFileIds) {

        final List<StaticFile> videoList = new ArrayList<StaticFile>();
        final ShardResult videoSR = shard();

        return sqlExecutor.openConnection(videoSR.db, new SingleConnectionHandler<List<StaticFile>>() {
            @Override
            protected List<StaticFile> handleConnection(Connection conn) {
                String sql = StaticFileSql.getStaticFiles(videoSR.table, staticFileIds);
                SqlExecutor.executeList(ctx, conn, sql, videoList, new ResultSetReader<StaticFile>() {
                    @Override
                    public StaticFile read(ResultSet rs, StaticFile reuse) throws SQLException {
                        return StaticFileRs.readStaticFile(rs, null);
                    }
                });
                return videoList;
            }
        });
    }

    public List<StaticFile> getStaticFileByUserIds(final Context ctx, final long... userId) {

        final List<StaticFile> videoList = new ArrayList<StaticFile>();
        final ShardResult staticFileSR = shard();

        return sqlExecutor.openConnection(staticFileSR.db, new SingleConnectionHandler<List<StaticFile>>() {
            @Override
            protected List<StaticFile> handleConnection(Connection conn) {
                String sql = StaticFileSql.getStaticFileByUserId(staticFileSR.table, userId);
                SqlExecutor.executeList(ctx, conn, sql, videoList, new ResultSetReader<StaticFile>() {
                    @Override
                    public StaticFile read(ResultSet rs, StaticFile reuse) throws SQLException {
                        return StaticFileRs.readStaticFile(rs, null);
                    }
                });
                return videoList;
            }
        });
    }
    
    public boolean deleteStaticFile(final Context ctx, final long... staticFile) {
        final ShardResult staticFileSR = shard();
        return sqlExecutor.openConnection(staticFileSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = StaticFileSql.deleteStaticFile(staticFileSR.table, staticFile);
                long n = SqlExecutor.executeUpdate(ctx, conn, sql);
                return n == 1;
            }
        });
    }
}
