package com.borqs.server.impl.migration.friend;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.sql.Sql;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.borqs.server.platform.sql.Sql.value;

public class FriendMigSql {

    public static String getFriend(Context ctx, String table) {
        return new Sql()
                .select("* ")
                .from(table)
                .where(" type = 0 ").toString();
    }

    public static List<String> insertFriends(String table, List<MigrationFriend> friends) {
        List<String> stringSqls = new ArrayList<String>();
        for (MigrationFriend friend : friends) {
            String sql = "";
            long friendId = friend.getFriend();
            if (friendId > 10000 && friendId < 99999)
                sql = new Sql().insertInto(table).values(
                        value("user", friend.getUser()),
                        value("type", friend.getType()),
                        value("friend", friend.getFriend()),
                        value("circle", friend.getCricle()),
                        value("updated_time", friend.getUpdated_time()),
                        value("reason", friend.getReason())
                ).toString();

            if (StringUtils.isNotEmpty(sql))
                stringSqls.add(sql);
        }
        return stringSqls;
    }

    public static List<String> insertFollowers(String table, List<MigrationFriend> friendList) {
        List<String> stringSqls = new ArrayList<String>();
        for (MigrationFriend friend : friendList) {
            String sql = "";
            long friendId = friend.getFriend();
            if (friendId > 10000 && friendId < 99999)
                sql = new Sql().insertInto(table).values(
                        value("circle", friend.getCricle()),
                        value("reason", friend.getReason()),
                        value("type", friend.getType()),
                        value("updated_time", 0),
                        value("friend", friend.getFriend()),
                        value("follower", friend.getUser())
                ).toString();

            if (StringUtils.isNotEmpty(sql))
                stringSqls.add(sql);
        }
        return stringSqls;
    }
}
