package com.borqs.server.impl.migration.friend;

import com.borqs.server.impl.migration.MigrationDb;
import com.borqs.server.impl.migration.MigrationSql;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FriendMigDb extends SqlSupport {
    private static final Logger L = Logger.get(FriendMigDb.class);
    // table
    private Table firendOldTable;
    private Table friendTable;
    private Table followerTable;

    public FriendMigDb() {
    }

    public void setFriendTable(Table friendTable) {
        this.friendTable = friendTable;
    }

    public void setFirendOldTable(Table firendOldTable) {
        this.firendOldTable = firendOldTable;
    }

    public Table getFriendTable() {
        return friendTable;
    }

    private ShardResult shardFriend() {
        return friendTable.getShard(0);
    }

    private ShardResult shardOldFriend() {
        return firendOldTable.getShard(0);
    }

    private ShardResult shardFollower() {
        return followerTable.getShard(0);
    }

    public void setFollowerTable(Table followerTable) {
        this.followerTable = followerTable;
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
                        return FriendMigRs.readFriend(rs, null);
                    }
                });
                return friendList;
            }
        });

        if (friendList.size() > 0)
            sqlExecutor.openConnection(friendSR.db, new SingleConnectionHandler<Boolean>() {
                @Override
                protected Boolean handleConnection(Connection conn) {
                    List<String> sqls = FriendMigSql.insertFriends(friendSR.table, friendList);
                    long commentId = SqlExecutor.executeUpdate(ctx, conn, sqls);

                    return true;
                }
            });

        followerMigration(ctx, friendList);
    }

    public void followerMigration(final Context ctx, final List<MigrationFriend> friendList) {
        final ShardResult followerSR = shardFollower();
        final LogCall LC = LogCall.startCall(L, MigrationDb.class, "followerMigration",
                ctx, "friendList", friendList);

        if (friendList.size() > 0)
            sqlExecutor.openConnection(followerSR.db, new SingleConnectionHandler<Boolean>() {
                @Override
                protected Boolean handleConnection(Connection conn) {
                    List<String> sqls = FriendMigSql.insertFollowers(followerSR.table, friendList);
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

