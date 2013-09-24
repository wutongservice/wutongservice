package com.borqs.server.impl.video;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.video.Video;
import com.borqs.server.platform.sql.*;
import org.apache.commons.lang.Validate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VideoDb extends SqlSupport {
    private Table videoTable;

    public VideoDb() {
    }


    public void setVideoTable(Table videoTable) {
        if (videoTable != null)
            Validate.isTrue(videoTable.getShardCount() == 1);
        this.videoTable = videoTable;
    }

    private ShardResult shard() {
        return videoTable.getShard(0);
    }


    public long saveVideo(final Context ctx, final Video video) {
        final ShardResult videoSR = shard();
        return sqlExecutor.openConnection(videoSR.db, new SingleConnectionHandler<Long>() {
            @Override
            protected Long handleConnection(Connection conn) {
                String sql = VideoSql.insertVideo(videoSR.table, video);
                return SqlExecutor.executeUpdate(ctx, conn, sql);
            }
        });
    }

    public List<Video> getVideos(final Context ctx, final long... videoIds) {

        final List<Video> videoList = new ArrayList<Video>();
        final ShardResult videoSR = shard();

        return sqlExecutor.openConnection(videoSR.db, new SingleConnectionHandler<List<Video>>() {
            @Override
            protected List<Video> handleConnection(Connection conn) {
                String sql = VideoSql.getVideos(videoSR.table, videoIds);
                SqlExecutor.executeList(ctx, conn, sql, videoList, new ResultSetReader<Video>() {
                    @Override
                    public Video read(ResultSet rs, Video reuse) throws SQLException {
                        return VideoRs.readVideo(rs, null);
                    }
                });
                return videoList;
            }
        });
    }

    public List<Video> getVideoByUserIds(final Context ctx, final long... userId) {

        final List<Video> videoList = new ArrayList<Video>();
        final ShardResult videoSR = shard();

        return sqlExecutor.openConnection(videoSR.db, new SingleConnectionHandler<List<Video>>() {
            @Override
            protected List<Video> handleConnection(Connection conn) {
                String sql = VideoSql.getVideoByUserId(videoSR.table, userId);
                SqlExecutor.executeList(ctx, conn, sql, videoList, new ResultSetReader<Video>() {
                    @Override
                    public Video read(ResultSet rs, Video reuse) throws SQLException {
                        return VideoRs.readVideo(rs, null);
                    }
                });
                return videoList;
            }
        });
    }

    public boolean deleteVideo(final Context ctx, final long... video) {
        final ShardResult videoSR = shard();
        return sqlExecutor.openConnection(videoSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = VideoSql.deleteVideo(videoSR.table, video);
                long n = SqlExecutor.executeUpdate(ctx, conn, sql);
                return n == 1;
            }
        });
    }
}
