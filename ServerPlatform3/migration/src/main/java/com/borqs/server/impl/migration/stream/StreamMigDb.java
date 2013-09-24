package com.borqs.server.impl.migration.stream;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.Posts;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.*;
import com.borqs.server.platform.util.CollectionsHelper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;

public class StreamMigDb extends SqlSupport {
    private static final Logger L = Logger.get(StreamMigDb.class);

    private Map<Long,String> userIdMap;
    // table
    private Table streamTable;

    public StreamMigDb() {
    }

    public void setUserIdMap(Map<Long, String> userIdMap) {
        this.userIdMap = userIdMap;
    }

    public Table getStreamTable() {
        return streamTable;
    }

    public void setStreamTable(Table streamTable) {
        this.streamTable = streamTable;
    }

    private ShardResult shardStream() {
        return streamTable.getShard(0);
    }

    public Posts getPost(final Context ctx) {
        final ShardResult streamSR = shardStream();

        return sqlExecutor.openConnection(streamSR.db, new SingleConnectionHandler<Posts>() {
            @Override
            protected Posts handleConnection(Connection conn) {

                final Posts postList = new Posts();
                String sql = StreamMigSql.getStream(ctx, streamSR.table);
                SqlExecutor.executeList(ctx, conn, sql, postList, new ResultSetReader<Post>() {
                    @Override
                    public Post read(ResultSet rs, Post reuse) throws SQLException {
                        return StreamMigRs.readStream(rs, null,userIdMap);
                    }
                });
                return postList;
            }
        });
    }

    public Posts checkPost(final Context ctx,final Posts n) {
        final ShardResult streamSR = shardStream();

        return sqlExecutor.openConnection(streamSR.db, new SingleConnectionHandler<Posts>() {
            @Override
            protected Posts handleConnection(Connection conn) {

                final Posts postList = new Posts();
                String sql = StreamMigSql.getStream(ctx, streamSR.table);
                SqlExecutor.executeList(ctx, conn, sql, postList, new ResultSetReader<Post>() {
                    @Override
                    public Post read(ResultSet rs, Post reuse) throws SQLException {
                        return StreamMigRs.checkStream(rs, n,userIdMap);
                    }
                });
                return postList;
            }
        });
    }

    public long[] getAllPostIds(final Context ctx) {
        final ShardResult streamSR = shardStream();
        return sqlExecutor.openConnection(streamSR.db, new SingleConnectionHandler<long[]>() {
            @Override
            protected long[] handleConnection(Connection conn) {
                String sql = StreamMigSql.findAllPostIds(streamSR.table);
                final HashSet<Long> existsIds = new HashSet<Long>();
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        StreamMigRs.readIds(rs, existsIds);
                    }
                });
                return CollectionsHelper.toLongArray(existsIds);
            }
        });
    }
}

