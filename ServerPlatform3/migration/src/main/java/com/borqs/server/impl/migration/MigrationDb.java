package com.borqs.server.impl.migration;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.Posts;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MigrationDb extends SqlSupport {
    private static final Logger L = Logger.get(MigrationDb.class);
    // table
    private Table streamTable;

    private Table friendTable;

    private Table circleTable;

    private Table circleOldTable;

    private Table firendOldTable;

    private Table followerTable;

    private Table accountTable;

    private Table accountPropertyTable;


    public MigrationDb() {
    }

    public Table getStreamTable() {
        return streamTable;
    }

    public void setStreamTable(Table streamTable) {
        this.streamTable = streamTable;
    }

    public Table getFriendTable() {
        return friendTable;
    }

    public void setFriendTable(Table friendTable) {
        this.friendTable = friendTable;
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

    public Table getFirendOldTable() {
        return firendOldTable;
    }

    public void setFirendOldTable(Table firendOldTable) {
        this.firendOldTable = firendOldTable;
    }

    public Table getFollowerTable() {
        return followerTable;
    }

    public void setFollowerTable(Table followerTable) {
        this.followerTable = followerTable;
    }

    public Table getAccountTable() {
        return accountTable;
    }

    public void setAccountTable(Table accountTable) {
        this.accountTable = accountTable;
    }

    public Table getAccountPropertyTable() {
        return accountPropertyTable;
    }

    public void setAccountPropertyTable(Table accountPropertyTable) {
        this.accountPropertyTable = accountPropertyTable;
    }

    private ShardResult shardStream() {
        return streamTable.getShard(0);
    }

    private ShardResult shardFriend() {
        return friendTable.getShard(0);
    }

    private ShardResult shardOldFriend() {
        return firendOldTable.getShard(0);
    }

    private ShardResult shardCircle() {
        return circleTable.getShard(0);
    }

    private ShardResult shardOldCircle() {
        return circleOldTable.getShard(0);
    }

    private ShardResult shardFollower() {
        return followerTable.getShard(0);
    }

    private ShardResult shardAccount() {
        return accountTable.getShard(0);
    }




    public List<User> getAccounts(final Context ctx) {
        final ShardResult accountSR = shardAccount();
        final List<User> postList = new ArrayList<User>();
        final List<String> errorList = new ArrayList<String>();

        return sqlExecutor.openConnection(accountSR.db, new SingleConnectionHandler<List<User>>() {
            @Override
            protected List<User> handleConnection(Connection conn) {


                String sql = MigrationSql.getAccount(ctx, accountSR.table);
                SqlExecutor.executeList(ctx, conn, sql, postList, new ResultSetReader<User>() {
                    @Override
                    public User read(ResultSet rs, User reuse) throws SQLException {
                        return MigrationRs.readAccount(rs, errorList);
                    }
                });
                System.out.print("----------------------------------");
                System.out.print("-----------------"+errorList.toString()+"-----------------");
                System.out.print("----------------------------------");
                return postList;
            }
        });


    }


    public Posts getPost(final Context ctx) {
        final ShardResult streamSR = shardStream();

        return sqlExecutor.openConnection(streamSR.db, new SingleConnectionHandler<Posts>() {
            @Override
            protected Posts handleConnection(Connection conn) {

                final Posts postList = new Posts();
                String sql = MigrationSql.getStream(ctx, streamSR.table);
                SqlExecutor.executeList(ctx, conn, sql, postList, new ResultSetReader<Post>() {
                    @Override
                    public Post read(ResultSet rs, Post reuse) throws SQLException {
                        return MigrationRs.readStream(rs, null);
                    }
                });
                return postList;
            }
        });
    }

    public void friendMigration(final Context ctx) {
        final ShardResult friendSR = shardFriend();
        final ShardResult friendOldSR = shardOldFriend();
        final List<MigrationFriend> friendList = new ArrayList<MigrationFriend>();

        sqlExecutor.openConnection(friendOldSR.db, new SingleConnectionHandler<List<MigrationFriend>>() {
            @Override
            protected List<MigrationFriend> handleConnection(Connection conn) {


                String sql = MigrationSql.getFriend(ctx, friendOldSR.table);
                SqlExecutor.executeList(ctx, conn, sql, friendList, new ResultSetReader<MigrationFriend>() {
                    @Override
                    public MigrationFriend read(ResultSet rs, MigrationFriend reuse) throws SQLException {
                        return MigrationRs.readFriend(rs, null);
                    }
                });
                return friendList;
            }
        });

        if (friendList.size() > 0)
            sqlExecutor.openConnection(friendSR.db, new SingleConnectionHandler<Boolean>() {
                @Override
                protected Boolean handleConnection(Connection conn) {
                    List<String> sqls = MigrationSql.insertFriends(friendSR.table, friendList);
                    long commentId = SqlExecutor.executeUpdate(ctx, conn, sqls);

                    return true;
                }
            });

        followerMigration(ctx, friendList);
    }

    public void circleMigration(final Context ctx) {
        final ShardResult circleSR = shardCircle();
        final ShardResult circleOldSR = shardOldCircle();

        final List<MigrationCircle> circleList = new ArrayList<MigrationCircle>();

        sqlExecutor.openConnection(circleOldSR.db, new SingleConnectionHandler<List<MigrationCircle>>() {
            @Override
            protected List<MigrationCircle> handleConnection(Connection conn) {


                String sql = MigrationSql.getFriend(ctx, circleOldSR.table);
                SqlExecutor.executeList(ctx, conn, sql, circleList, new ResultSetReader<MigrationCircle>() {
                    @Override
                    public MigrationCircle read(ResultSet rs, MigrationCircle reuse) throws SQLException {
                        return MigrationRs.readCircle(rs, null);
                    }
                });
                return circleList;
            }
        });

        if (circleList.size() > 0)
            sqlExecutor.openConnection(circleSR.db, new SingleConnectionHandler<Boolean>() {
                @Override
                protected Boolean handleConnection(Connection conn) {
                    List<String> sqls = MigrationSql.insertCircles(circleSR.table, circleList);
                    long commentId = SqlExecutor.executeUpdate(ctx, conn, sqls);

                    return true;
                }
            });
    }

    public void followerMigration(final Context ctx, final List<MigrationFriend> friendList) {
        final ShardResult followerSR = shardFollower();
        final LogCall LC = LogCall.startCall(L, MigrationDb.class, "followerMigration",
                ctx, "friendList", friendList);

        if (friendList.size() > 0)
            sqlExecutor.openConnection(followerSR.db, new SingleConnectionHandler<Boolean>() {
                @Override
                protected Boolean handleConnection(Connection conn) {
                    List<String> sqls = MigrationSql.insertFollowers(followerSR.table, friendList);
                    for (String sql : sqls) {
                        try {
                            SqlExecutor.executeUpdate(ctx, conn, sql);
                        } catch (Exception e) {
                            LC.endCall(e);
                        }
                    }
                    return true;
                }
            });
    }


}

