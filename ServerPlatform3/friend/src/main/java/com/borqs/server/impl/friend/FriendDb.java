package com.borqs.server.impl.friend;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.friend.*;
import com.borqs.server.platform.sql.*;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.ObjectHolder;
import org.apache.commons.lang.ArrayUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FriendDb extends SqlSupport {

    // table
    private Table circleTable;
    private Table friendTable;
    private Table followerTable;
    private Table remarkTable;

    public FriendDb() {
    }

    public Table getCircleTable() {
        return circleTable;
    }

    public void setCircleTable(Table circleTable) {
        this.circleTable = circleTable;
    }

    public Table getFriendTable() {
        return friendTable;
    }

    public void setFriendTable(Table friendTable) {
        this.friendTable = friendTable;
    }

    public Table getFollowerTable() {
        return followerTable;
    }

    public void setFollowerTable(Table followerTable) {
        this.followerTable = followerTable;
    }

    public Table getRemarkTable() {
        return remarkTable;
    }

    public void setRemarkTable(Table remarkTable) {
        this.remarkTable = remarkTable;
    }

    private ShardResult shardFriend(long userId) {
        return friendTable.shard(userId);
    }

    private ShardResult shardFollower(PeopleId friendId) {
        return followerTable.shard(friendId);
    }

    private GroupedShardResults shardFollowers(PeopleId... friendIds) {
        return TableHelper.shard(followerTable, Arrays.asList(friendIds));
    }

    private GroupedShardResults shardFollowers(PeopleIds friendIds) {
        return TableHelper.shard(followerTable, friendIds);
    }

    private ShardResult shardCircle(long userId) {
        return circleTable.shard(userId);
    }

    private ShardResult shardRemark(long userId) {
        return remarkTable.shard(userId);
    }

    public AbstractFriendImpl.FriendEntries getFriendEntries(final Context ctx, final long userId) {
        final ShardResult circleSR = shardCircle(userId);
        final ShardResult friendSR = shardFriend(userId);
        final ShardResult remarkSR = shardRemark(userId);

        final AbstractFriendImpl.FriendEntries fes = new AbstractFriendImpl.FriendEntries();
        sqlExecutor.openConnections(circleSR.db, friendSR.db, remarkSR.db, new ConnectionsHandler<Object>() {
            @Override
            public Object handle(Connection[] conns) {
                Connection circleConn = conns[0];
                Connection friendConn = conns[1];
                Connection remarkConn = conns[2];
                getFriendEntries0(fes, ctx, userId, circleConn, friendConn, remarkConn,
                        circleSR.table, friendSR.table, remarkSR.table);
                return null;
            }
        });

        return fes;
    }

    private static void getFriendEntries0(final AbstractFriendImpl.FriendEntries fes, final Context ctx, long userId,
                                          Connection circleConn, Connection friendConn, Connection remarkConn,
                                          String circleTable, String friendTable, String remarkTable) {
        String getCircleSql = FriendSql.getCircles(circleTable, userId);
        String getFriendSql = FriendSql.getFriends(friendTable, userId);

        SqlExecutor.executeCustom(ctx, friendConn, getFriendSql, new ResultSetHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                FriendRs.readFriendEntries(rs, fes);
            }
        });

        SqlExecutor.executeCustom(ctx, circleConn, getCircleSql, new ResultSetHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                FriendRs.fillCircles(rs, fes, ctx.getLocale());
            }
        });
    }

    public Circle createCustomCircle(final Context ctx, final String circleName) {
        final long viewerId = ctx.getViewer();
        final ShardResult sr = shardCircle(viewerId);

        return sqlExecutor.openConnection(sr.db, new SingleConnectionHandler<Circle>() {
            @Override
            protected Circle handleConnection(Connection conn) {
                String sql = FriendSql.getCircles(sr.table, viewerId);
                final ObjectHolder<int[]> customCircleIds = new ObjectHolder<int[]>(null);
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        customCircleIds.value = FriendRs.getCircleIds(rs, Circle.MIN_CUSTOM_CIRCLE_ID);
                    }
                });
                int newCircleId = AbstractFriendImpl.newCircleId(customCircleIds.value);
                if (newCircleId < Circle.MIN_CUSTOM_CIRCLE_ID || newCircleId > Circle.MAX_CUSTOM_CIRCLE_ID)
                    throw new ServerException(E.TOO_MANY_CIRCLES, "Too many circles");

                Circle circle = new Circle(newCircleId, circleName, DateHelper.nowMillis());
                sql = FriendSql.createCustomCircle(sr.table, viewerId, circle);
                SqlExecutor.executeUpdate(ctx, conn, sql);
                return circle;
            }
        });
    }



    public boolean destroyCustomCircle(final Context ctx, final int circleId) {
        final long viewerId = ctx.getViewer();

        final ShardResult friendSR = shardFriend(viewerId);

        AbstractFriendImpl.FriendEntries fes = getFriendEntries(ctx, viewerId);
        if (!fes.hasCircle(circleId))
            return false;

        PeopleIds friendIds = fes.getFriendIds(null, circleId);
        if (!friendIds.isEmpty()) {
            GroupedShardResults followerSRs = shardFollowers(friendIds);
            for (final ShardResult followerSR : followerSRs.getShardResults()) {
                List<PeopleIds> friendIds1 = followerSRs.get(followerSR);
                sqlExecutor.openConnections(friendSR.db, followerSR.db, new ConnectionsHandler<Object>() {
                    @Override
                    public Object handle(Connection[] conns) {
                        Connection friendConn = conns[0];
                        Connection followerConn = conns[1];

                        String friendSql = FriendSql.destroyCircleInFriends(friendSR.table, viewerId, circleId);
                        String followerSql = FriendSql.destroyCircleInFollowers(followerSR.table, viewerId, circleId);

                        SqlExecutor.executeUpdate(ctx, friendConn, friendSql);
                        SqlExecutor.executeUpdate(ctx, followerConn, followerSql);
                        return null;
                    }
                });
            }
        }

        final ShardResult circleSR = shardCircle(viewerId);
        sqlExecutor.openConnection(circleSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = FriendSql.destroyCircle(circleSR.table, viewerId, circleId);
                SqlExecutor.executeUpdate(ctx, conn, sql);
                return null;
            }
        });
        return true;
    }


    public boolean updateCustomCircleName(final Context ctx, final int circleId, final String circleName) {
        final long viewerId = ctx.getViewer();
        final ShardResult sr = shardCircle(viewerId);

        return sqlExecutor.openConnection(sr.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = FriendSql.updateCircleName(sr.table, viewerId, circleId, circleName);
                long n = SqlExecutor.executeUpdate(ctx, conn, sql);
                return n > 0;
            }
        });
    }


    private static boolean checkCircleIds(Connection circleConn, Context ctx, String circleTable, long userId, int[] circleIds) {
        boolean hasCustomCircleIds = false;
        for (int circleId : circleIds) {
            if (Circle.isCustomCircleId(circleId)) {
                hasCustomCircleIds = true;
            } else {
                if (!Circle.isBuiltinActualCircle(circleId))
                    return false;
            }
        }
        if (!hasCustomCircleIds)
            return true;


        final ObjectHolder<int[]> customCircleIds = new ObjectHolder<int[]>(null);
        String sql = FriendSql.getCircles(circleTable, userId);
        SqlExecutor.executeCustom(ctx, circleConn, sql, new ResultSetHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                customCircleIds.value = FriendRs.getCircleIds(rs, Circle.MIN_CUSTOM_CIRCLE_ID);
            }
        });
        if (customCircleIds.value != null) {
            for (int circleId : circleIds) {
                if (Circle.isCustomCircleId(circleId)) {
                    if (!ArrayUtils.contains(customCircleIds.value, circleId))
                        return false;
                }
            }
        }
        return true;
    }

    public void setFriendIntoCircles(final Context ctx, final int reason, final PeopleId friendId, final int... circleIds) {
        final long viewerId = ctx.getViewer();
        final ShardResult circleSR = shardCircle(viewerId);
        final ShardResult friendSR = shardFriend(viewerId);
        final ShardResult followerSR = shardFollower(friendId);
        final long now = DateHelper.nowMillis();

        sqlExecutor.openConnections(circleSR.db, friendSR.db, followerSR.db, new ConnectionsHandler<Object>() {
            @Override
            public Object handle(Connection[] conns) {
                Connection circleConn = conns[0];
                Connection friendConn = conns[1];
                Connection followerConn = conns[2];

                if (!checkCircleIds(circleConn, ctx, circleSR.table, viewerId, circleIds))
                    throw new ServerException(E.INVALID_CIRCLE, "Illegal circle");

                List<String> friendSqls = FriendSql.setFriendInCircles(
                        friendSR.table, viewerId, reason, friendId, circleIds, now);
                List<String> followerSqls = FriendSql.setFollowerInCircles(
                        followerSR.table, friendId, reason, viewerId, circleIds, now);

                SqlExecutor.executeUpdate(ctx, friendConn, friendSqls);
                SqlExecutor.executeUpdate(ctx, followerConn, followerSqls);
                return null;
            }
        }
        );
    }

    private boolean checkCircleId(final Context ctx, final int circleId) {
        final ShardResult circleSR = shardCircle(ctx.getViewer());
        if (Circle.isCustomCircleId(circleId)) {
            return sqlExecutor.openConnection(circleSR.db, new SingleConnectionHandler<Boolean>() {
                @Override
                protected Boolean handleConnection(Connection conn) {
                    String sql = FriendSql.getCircleIds(circleSR.table, ctx.getViewer());
                    final ObjectHolder<int[]> customCircleIds = new ObjectHolder<int[]>(null);
                    SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                        @Override
                        public void handle(ResultSet rs) throws SQLException {
                            customCircleIds.value = FriendRs.getCircleIds(rs, Circle.MIN_CUSTOM_CIRCLE_ID);
                        }
                    });
                    return customCircleIds.value == null || ArrayUtils.contains(customCircleIds.value, circleId);
                }
            });
        } else {
            return true;
        }
    }

    public void addFriendsIntoCircle(final Context ctx, final int reason, final PeopleIds friendIds, final int circleId) {
        if (!checkCircleId(ctx, circleId))
            throw new ServerException(E.INVALID_CIRCLE, "Illegal circle");

        final long viewerId = ctx.getViewer();
        final ShardResult friendSR = shardFriend(viewerId);
        final GroupedShardResults followerSRs = shardFollowers(friendIds);
        final long now = DateHelper.nowMillis();


        for (final ShardResult followerSR : followerSRs.getShardResults()) {
            final List<PeopleId> friendIds0 = followerSRs.get(followerSR);
            sqlExecutor.openConnections(friendSR.db, followerSR.db, new ConnectionsHandler<Object>() {
                @Override
                public Object handle(Connection[] conns) {
                    Connection friendConn = conns[0];
                    Connection followerConn = conns[1];

                    List<String> friendSqls = FriendSql.addFriendsIntoCircle(
                            friendSR.table, viewerId, reason, friendIds0, circleId, now);
                    List<String> followerSqls = FriendSql.addFollowersIntoCircle(
                            followerSR.table, friendIds0, reason, viewerId, circleId, now);

                    SqlExecutor.executeUpdate(ctx, friendConn, friendSqls);
                    SqlExecutor.executeUpdate(ctx, followerConn, followerSqls);
                    return null;
                }
            });
        }

    }

    public void removeFriendsInCircle(final Context ctx, final PeopleIds friendIds, final int circleId) {
        if (!checkCircleId(ctx, circleId))
            throw new ServerException(E.INVALID_CIRCLE, "Illegal circle");

        final long viewerId = ctx.getViewer();
        final ShardResult friendSR = shardFriend(viewerId);
        final GroupedShardResults followerSRs = shardFollowers(friendIds);

        for (final ShardResult followerSR : followerSRs.getShardResults()) {
            final List<PeopleId> friendIds0 = followerSRs.get(followerSR);
            sqlExecutor.openConnections(friendSR.db, followerSR.db, new ConnectionsHandler<Object>() {
                @Override
                public Object handle(Connection[] conns) {
                    Connection friendConn = conns[0];
                    Connection followerConn = conns[1];

                    List<String> friendSqls = FriendSql.removeFriendsInCircle(
                            friendSR.table, viewerId, friendIds0, circleId);
                    List<String> followerSqls = FriendSql.removeFollowersInCircle(
                            followerSR.table, friendIds0, viewerId, circleId);

                    SqlExecutor.executeUpdate(ctx, friendConn, friendSqls);
                    SqlExecutor.executeUpdate(ctx, followerConn, followerSqls);
                    return null;
                }
            });
        }
    }

    public int getFollowersCount(final Context ctx, final PeopleId friendId) {
        final ShardResult sr = shardFollower(friendId);
        return sqlExecutor.openConnection(sr.db, new SingleConnectionHandler<Integer>() {
            @Override
            protected Integer handleConnection(Connection conn) {
                String sql = FriendSql.getFollowersCount(sr.table, friendId);
                long count = SqlExecutor.executeInt(ctx, conn, sql, 0L);
                return (int) count;
            }
        });
    }

    public long[] getFollowers(final Context ctx, final PeopleId friendId, final Page page) {
        final ShardResult sr = shardFollower(friendId);
        return sqlExecutor.openConnection(sr.db, new SingleConnectionHandler<long[]>() {
            @Override
            protected long[] handleConnection(Connection conn) {
                String sql = FriendSql.getFollowers(sr.table, friendId, page);
                final ObjectHolder<long[]> followerIds = new ObjectHolder<long[]>(null);
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        followerIds.value = FriendRs.getFollowerIds(rs);
                    }
                });
                return followerIds.value;
            }
        });
    }

    public void getRelationships(final Relationships result, final Context ctx, final PeopleId viewer, final PeopleId... targets) {

        // fill default
        for (PeopleId target : targets)
            result.add(Relationship.disrelated(viewer, target));

        if (viewer.isUser()) {
            final ShardResult friendSR = shardFriend(viewer.getIdAsLong());
            sqlExecutor.openConnection(friendSR.db, new SingleConnectionHandler<Object>() {
                @Override
                protected Object handleConnection(Connection conn) {
                    String sql = FriendSql.getRelationsInFriends(friendSR.table, viewer.getIdAsLong(), targets);
                    SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                        @Override
                        public void handle(ResultSet rs) throws SQLException {
                            FriendRs.fillRelationsInFriends(rs, result);
                        }
                    });
                    return null;
                }
            });
        }

        final long[] targetIds = new PeopleIds(targets).getIdsAsLongArray(PeopleId.USER);
        if (ArrayUtils.isNotEmpty(targetIds)) {
            final ShardResult followerSR = shardFollower(viewer);
            sqlExecutor.openConnection(followerSR.db, new SingleConnectionHandler<Object>() {
                @Override
                protected Object handleConnection(Connection conn) {
                    String sql = FriendSql.getRelationsInFollower(followerSR.table, viewer, targetIds);
                    SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                        @Override
                        public void handle(ResultSet rs) throws SQLException {
                            FriendRs.fillRelationsInFollowers(rs, result);
                        }
                    });
                    return null;
                }
            });
        }
    }

    public Map<PeopleId, String> getRemarks(final Context ctx, final long userId) {
        final ShardResult sr = shardRemark(userId);
        final HashMap<PeopleId, String> remarks = new HashMap<PeopleId, String>();
        sqlExecutor.openConnection(sr.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = FriendSql.getRemarks(sr.table, userId);
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        FriendRs.fillRemarks(rs, remarks);
                    }
                });
                return null;
            }
        });
        return remarks;
    }

    public void setRemark(final Context ctx, final PeopleId friendId, final String remark) {
        final long viewerId = ctx.getViewer();
        final ShardResult sr = shardRemark(viewerId);
        sqlExecutor.openConnection(sr.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = FriendSql.setRemark(sr.table, viewerId, friendId, remark, DateHelper.nowMillis());
                SqlExecutor.executeUpdate(ctx, conn, sql);
                return null;
            }
        });
    }
}
