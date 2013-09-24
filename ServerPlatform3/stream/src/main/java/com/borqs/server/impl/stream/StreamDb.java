package com.borqs.server.impl.stream;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.PostFilter;
import com.borqs.server.platform.feature.stream.Posts;
import com.borqs.server.platform.sql.*;
import com.borqs.server.platform.util.CollectionsHelper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class StreamDb extends SqlSupport {
    // table
    private Table postTable;


    public StreamDb() {
    }

    public Table getPostTable() {
        return postTable;
    }

    public void setPostTable(Table postTable) {
        this.postTable = postTable;
    }


    private ShardResult shardPost(long streamId) {
        return postTable.shard(streamId);
    }

    private ShardResult shardPost() {
        return postTable.getShard(0);
    }


    private GroupedShardResults shardPost(long... ids) {
        return TableHelper.shard(postTable, ids);
    }

    public Post createStream(final Context ctx, final Post stream) {
        final ShardResult streamSR = shardPost();

        return sqlExecutor.openConnection(streamSR.db, new SingleConnectionHandler<Post>() {
            @Override
            protected Post handleConnection(Connection conn) {
                if (stream.getPostId() == 0)
                    return null;
                String sql = StreamSql.saveStream(streamSR.table, stream);
                SqlExecutor.executeUpdate(ctx, conn, sql);

                return stream;
            }
        });
    }

    public boolean destroyPosts(final Context ctx, final long... posts) {
        // to make sure destroy operator's record
        final long viewId = ctx.getViewer();
        final ShardResult postSR = shardPost();
        return sqlExecutor.openConnection(postSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = StreamSql.destroyedPosts(ctx,postSR.table, viewId, posts);
                SqlExecutor.executeUpdate(ctx, conn, sql);

                return true;
            }
        });
    }

    public boolean updateStream(final Context ctx, final Post post) {
        final ShardResult postSR = shardPost(post.getPostId());

        return sqlExecutor.openConnection(postSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = StreamSql.updatePost(postSR.table, post);
                long l = SqlExecutor.executeUpdate(ctx, conn, sql);
                if (l > 0)
                    return true;
                else
                    return false;
            }
        });
    }

    public boolean hasAllPost(final Context ctx, final long... postId) {
        long postIds = postId.length;
        GroupedShardResults groupedPropSR = shardPost(postId);

        for (final ShardResult postSR : groupedPropSR.getShardResults()) {
            long s = sqlExecutor.openConnection(postSR.db, new SingleConnectionHandler<Long>() {
                @Override
                protected Long handleConnection(Connection conn) {
                    String sql = StreamSql.hasAllPosts(postSR.table, postId);
                    long num = SqlExecutor.executeInt(ctx, conn, sql, 0);

                    return num;
                }
            });
            postIds -= s;

        }
        return postIds == 0;
    }

    public boolean hasAnyPost(final Context ctx, final long... postId) {
        long postIds = postId.length;
        GroupedShardResults groupedPropSR = shardPost(postId);

        for (final ShardResult postSR : groupedPropSR.getShardResults()) {
            long b = sqlExecutor.openConnection(postSR.db, new SingleConnectionHandler<Long>() {
                @Override
                protected Long handleConnection(Connection conn) {
                    String sql = StreamSql.hasAnyPosts(postSR.table, postId);
                    long num = SqlExecutor.executeInt(ctx, conn, sql, 0);

                    return num;
                }
            });
            postIds -= b;
        }
        return postIds < postId.length;
    }

    public Posts getPosts(final Context ctx, final long... postId) {

        final Posts posts = new Posts();
        GroupedShardResults groupedPropSR = shardPost(postId);

        for (final ShardResult postSR : groupedPropSR.getShardResults()) {
             sqlExecutor.openConnection(postSR.db, new SingleConnectionHandler<Posts>() {
                @Override
                protected Posts handleConnection(Connection conn) {
                    String sql = StreamSql.getPosts(postSR.table, postId);
                    SqlExecutor.executeList(ctx, conn, sql, posts, new ResultSetReader<Post>() {
                        @Override
                        public Post read(ResultSet rs, Post reuse) throws SQLException {
                            return StreamRs.readStream(rs, null);
                        }
                    });
                    return posts;
                }
            });
        }
        return posts;
    }

    public long[] getPublicTimelinePostIds(final Context ctx, final PostFilter filter, final Page page) {
        final ShardResult postSR = shardPost();
        final ArrayList<Long> postIds = new ArrayList<Long>();
        sqlExecutor.openConnection(postSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = StreamSql.getPublicTimeline(ctx, postSR.table, filter, page);
                SqlExecutor.executeList(ctx, conn, sql, postIds, new ResultSetReader<Long>() {
                    @Override
                    public Long read(ResultSet rs, Long reuse) throws SQLException {
                        return rs.getLong("post_id");
                    }
                });
                return null;
            }
        });
        return CollectionsHelper.toLongArray(postIds);
    }

    public Posts getPostForTimeLine(final Context ctx, final String table) {
        final ShardResult postSR = shardPost();

        final Posts posts = new Posts();
        return sqlExecutor.openConnection(postSR.db, new SingleConnectionHandler<Posts>() {
            @Override
            protected Posts handleConnection(Connection conn) {
                String sql = StreamSql.getPostIds(table);
                SqlExecutor.executeList(ctx, conn, sql, posts, new ResultSetReader<Post>() {
                    @Override
                    public Post read(ResultSet rs, Post reuse) throws SQLException {
                        return StreamRs.readStream(rs, null);
                    }
                });
                return posts;
            }
        });
    }
}

