package com.borqs.server.impl.migration;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.sql.Sql;

import java.util.ArrayList;
import java.util.List;

import static com.borqs.server.platform.sql.Sql.value;


public class MigrationSql {

    public static String getAccount(Context ctx, String table) {
        return new Sql()
                .select("* ")
                .from(table)
                .where(" 1=1 ").toString();
    }
    
    public static String getStream(Context ctx, String table) {
        return new Sql()
                .select("* ")
                .from(table)
                .where(" 1=1 ").toString();
    }


    public static String getFriend(Context ctx, String table) {
        return new Sql()
                .select("* ")
                .from(table)
                .where(" type = 0 ").toString();
    }

    public static List<String> insertFriends(String table, List<MigrationFriend> friends) {
        List<String> stringSqls = new ArrayList<String>();
        for (MigrationFriend friend : friends) {

            String sql = new Sql().insertInto(table).values(
                    value("user", friend.getUser()),
                    value("type", friend.getType()),
                    value("friend", friend.getFriend()),
                    value("circle", friend.getCricle()),
                    value("updated_time", friend.getUpdated_time()),
                    value("reason", friend.getReason())
            ).toString();
            stringSqls.add(sql);
        }
        return stringSqls;
    }

    public static List<String> insertCircles(String table, List<MigrationCircle> circleList) {
        List<String> stringSqls = new ArrayList<String>();
        for (MigrationCircle circle : circleList) {

            String sql = new Sql().insertInto(table).values(
                    value("user", circle.getUser()),
                    value("circle_id", circle.getCircle()),
                    value("name",circle.getName()),
                    value("updated_time", circle.getUpdated_time())
            ).toString();
            stringSqls.add(sql);
        }
        return stringSqls;
    }

    public static List<String> insertFollowers(String table, List<MigrationFriend> friendList) {
        List<String> stringSqls = new ArrayList<String>();
        for (MigrationFriend friend : friendList) {

            String sql = new Sql().insertInto(table).values(
                    value("circle", friend.getCricle()),
                    value("reason", friend.getReason()),
                    value("type",friend.getType()),
                    value("updated_time", 0),
                    value("friend",friend.getFriend()),
                    value("follower",friend.getUser())
            ).toString();
            stringSqls.add(sql);
        }
        return stringSqls;
    }

}
