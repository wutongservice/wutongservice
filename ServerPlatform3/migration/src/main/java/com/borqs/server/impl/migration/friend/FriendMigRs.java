package com.borqs.server.impl.migration.friend;


import java.sql.ResultSet;
import java.sql.SQLException;

public class FriendMigRs {
    public static MigrationFriend readFriend(ResultSet rs, MigrationFriend friend0) throws SQLException {
        MigrationFriend friend = new MigrationFriend();

        long friendId = rs.getLong("friend");
        friend.setCricle(rs.getInt("circle"));
        friend.setFriend(friendId);
        friend.setReason(rs.getInt("reason"));
        friend.setType(rs.getInt("type") == 0 ? 1 : 2);
        friend.setUpdated_time(0);
        friend.setUser(rs.getLong("user"));
        return friend;
    }

}
