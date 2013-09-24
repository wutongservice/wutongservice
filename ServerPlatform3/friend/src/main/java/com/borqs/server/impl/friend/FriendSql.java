package com.borqs.server.impl.friend;


import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.friend.Circle;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.sql.Sql;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.borqs.server.platform.sql.Sql.value;

public class FriendSql {

    public static String getCircles(String table, long userId) {
        return new Sql()
                .select("circle_id", "name", "updated_time")
                .from(table)
                .where("user=:user_id", "user_id", userId)
                .toString();
    }

    public static String getFriends(String table, long userId) {
        return new Sql()
                .select("type", "friend", "circle", "reason", "updated_time")
                .from(table)
                .where("user=:user_id", "user_id", userId)
                .toString();
    }

    public static String getRemarks(String table, long userId) {
        return new Sql()
                .select("type", "friend", "remark", "updated_time")
                .from(table)
                .where("user=:user_id", "user_id", userId)
                .toString();
    }

    public static String createCustomCircle(String table, long userId, Circle circle) {
        return new Sql()
                .insertInto(table)
                .values(
                        value("`user`", userId),
                        value("circle_id", circle.getCircleId()),
                        value("`name`", circle.getCircleName()),
                        value("updated_time", circle.getUpdatedTime()))
                .toString();
    }

    public static String destroyCircle(String table, long userId, int circleId) {
        return new Sql()
                .deleteFrom(table)
                .where("user=:user_id AND circle_id=:circle_id", "user_id", userId, "circle_id", circleId)
                .toString();
    }

    public static String destroyCircleInFriends(String table, long userId, int circleId) {
        return new Sql()
                .deleteFrom(table)
                .where("user=:user_id AND circle=:circle_id", "user_id", userId, "circle_id", circleId)
                .toString();
    }

    public static String destroyCircleInFollowers(String table, long userId, int circleId) {
        return new Sql()
                .deleteFrom(table)
                .where("follower=:follower_id AND circle=:circle_id", "follower_id", userId, "circle_id", circleId)
                .toString();
    }

    public static String updateCircleName(String table, long userId, int circleId, String circleName) {
        return new Sql()
                .update(table)
                .setValues(value("`name`", circleName))
                .where("user=:user_id AND circle_id=:circle_id", "user_id", userId, "circle_id", circleId)
                .toString();
    }

//    public static String destroyRelationshipByCircle(String table, long userId, int circleId) {
//        return new Sql()
//                .deleteFrom(table)
//                .where("user=:user_id AND circle_id=")
//                .toString();
//    }

    public static String getCircleIds(String table, long userId) {
        return new Sql()
                .select("circle_id")
                .from(table)
                .where("user=:user_id", "user_id", userId)
                .toString();
    }

    public static List<String> setFriendInCircles(String table, long userId, int reason, PeopleId friendId,
                                                  int[] circleIds, long now) {
        ArrayList<String> sqls = new ArrayList<String>();
        sqls.add(new Sql()
                .deleteFrom(table)
                .where("user=:user_id AND type=:friend_type AND friend=:friend_id",
                        "user_id", userId, "friend_type", friendId.type, "friend_id", friendId.id)
                .toString());
        for (int circleId : circleIds) {
            sqls.add(new Sql()
                    .insertInto(table)
                    .values(
                            value("`user`", userId),
                            value("`type`", friendId.type),
                            value("`friend`", friendId.id),
                            value("circle", circleId),
                            value("reason", reason),
                            value("updated_time", now)
                    )
                    .toString());
        }
        return sqls;
    }

    public static List<String> setFollowerInCircles(String table, PeopleId friendId, int reason, long followerId,
                                                    int[] circleIds, long now) {
        ArrayList<String> sqls = new ArrayList<String>();
        sqls.add(new Sql()
                .deleteFrom(table)
                .where("`type`=:friend_type AND `friend`=:friend_id AND follower=:follower_id",
                        "friend_type", friendId.type, "friend_id", friendId.id, "follower_id", followerId)
                .toString());
        for (int circleId : circleIds) {
            sqls.add(new Sql()
                    .insertInto(table)
                    .values(
                            value("`type`", friendId.type),
                            value("`friend`", friendId.id),
                            value("follower", followerId),
                            value("circle", circleId),
                            value("reason", reason),
                            value("updated_time", now)
                    )
                    .toString());
        }
        return sqls;
    }

    public static List<String> addFriendsIntoCircle(String table, long userId, int reason, List<PeopleId> friendIds,
                                                    int circleId, long now) {
        ArrayList<String> sqls = new ArrayList<String>();
        for (PeopleId friendId : friendIds) {
            sqls.add(new Sql()
                    .insertInto(table)
                    .values(
                            value("`user`", userId),
                            value("`type`", friendId.type),
                            value("`friend`", friendId.id),
                            value("`circle`", circleId),
                            value("reason", reason),
                            value("updated_time", now)
                    )
                    .onDuplicateKey()
                    .update()
                    .pairValues(
                            value("reason", reason),
                            value("updated_time", now)
                    )
                    .toString());
        }
        return sqls;
    }

    public static List<String> addFollowersIntoCircle(String table, List<PeopleId> friendIds, int reason, long followerId,
                                                      int circleId, long now) {
        ArrayList<String> sqls = new ArrayList<String>();
        for (PeopleId friendId : friendIds) {
            sqls.add(new Sql()
                    .insertInto(table)
                    .values(
                            value("`type`", friendId.type),
                            value("`friend`", friendId.id),
                            value("follower", followerId),
                            value("circle", circleId),
                            value("reason", reason),
                            value("updated_time", now)
                    )
                    .onDuplicateKey()
                    .update()
                    .pairValues(
                            value("reason", reason),
                            value("updated_time", now)
                    )
                    .toString());
        }
        return sqls;
    }

    public static List<String> removeFriendsInCircle(String table,
                                                     long userId, List<PeopleId> friendIds, int circleId) {
        ArrayList<String> sqls = new ArrayList<String>();
        for (PeopleId friendId : friendIds) {
            sqls.add(new Sql()
                    .deleteFrom(table)
                    .where("`user`=:user_id AND `type`=:friend_type AND `friend`=:friend_id AND circle=:circle_id",
                            "user_id", userId, "friend_type", friendId.type, "friend_id", friendId.id, "circle_id", circleId)
                    .toString());
        }
        return sqls;
    }

    public static List<String> removeFollowersInCircle(String table,
                                                       List<PeopleId> friendIds, long followerId, int circleId) {
        ArrayList<String> sqls = new ArrayList<String>();
        for (PeopleId friendId : friendIds) {
            sqls.add(new Sql()
                    .deleteFrom(table)
                    .where("`type`=:friend_type AND `friend`=:friend_id AND follower=:follower_id AND circle=:circle_id",
                            "friend_type", friendId.type, "friend_id", friendId.id, "follower_id", followerId, "circle_id", circleId)
                    .toString());
        }
        return sqls;
    }

    public static String getFollowersCount(String table, PeopleId friendId) {
        return new Sql()
                .select("COUNT(DISTINCT(follower))")
                .from(table)
                .where("`type`=:friend_type AND `friend`=:friend_id AND circle<>:blocked_circle_id",
                        "friend_type", friendId.type, "friend_id", friendId.id, "blocked_circle_id", Circle.CIRCLE_BLOCKED)
                .toString();
    }

    public static String getFollowers(String table, PeopleId friendId, Page page) {
        return new Sql()
                .select("follower")
                .from(table)
                .where("`type`=:friend_type AND `friend`=:friend_id AND circle<>:blocked_circle_id",
                        "friend_type", friendId.type, "friend_id", friendId.id, "blocked_circle_id", Circle.CIRCLE_BLOCKED)
                .groupBy("follower")
                .orderBy("MAX(updated_time)", "DESC")
                .page(page)
                .toString();
    }

    public static String setRemark(String table, long userId, PeopleId friendId, String remark, long now) {
        if (StringUtils.isEmpty(remark)) {
            return new Sql()
                    .deleteFrom(table)
                    .where("`user`=:user_id AND `type`=:friend_type AND `friend`=:friend_id",
                            "user_id", userId, "friend_type", friendId.type, "friend_id", friendId.id)
                    .toString();
        } else {
            return new Sql()
                    .insertInto(table)
                    .values(
                            value("`user`", userId),
                            value("`type`", friendId.type),
                            value("`friend`", friendId.id),
                            value("`remark`", remark),
                            value("updated_time", now)
                    )
                    .onDuplicateKey()
                    .update()
                    .pairValues(
                            value("`remark`", remark),
                            value("updated_time", now)
                    )
                    .toString();
        }
    }

    public static String getRelationsInFriends(String table, long viewerId, PeopleId[] targets) {
        if (targets.length == 1) {
            PeopleId target = targets[0];
            return new Sql()
                    .select("`user`", "`type`", "`friend`", "circle", "updated_time", "reason")
                    .from(table)
                    .where("`user`=:viewer_id AND `type`=:friend_type AND `friend`=:friend_id",
                            "viewer_id", viewerId, "friend_type", target.type, "friend_id", target.id)
                    .toString();
        } else {
            Map<Integer, PeopleIds> grouped = PeopleIds.group(targets);
            ArrayList<Sql> sqls = new ArrayList<Sql>();
            for (Map.Entry<Integer, PeopleIds> e : grouped.entrySet()) {
                int friendType = e.getKey();
                String[] ids = e.getValue().getIds(friendType);
                sqls.add(new Sql()
                        .select("`user`", "`type`", "`friend`", "circle", "updated_time", "reason")
                        .from(table)
                        .where("`user`=:viewer_id AND `type`=:friend_type AND `friend` IN ($friend_ids)",
                                "viewer_id", viewerId, "friend_type", friendType, "friend_ids", Sql.joinSqlValues(ids, ",")));
            }
            return sqls.size() == 1 ? sqls.get(0).toString() : Sql.unionAll(sqls).toString();
        }
    }

    public static String getRelationsInFollower(String table, PeopleId viewer, long[] targetIds) {
        Sql sql = new Sql()
                .select("`type`", "`friend`", "`follower`", "circle", "updated_time", "reason")
                .from(table);
        if (targetIds.length == 1) {
            sql.where("`type`=:friend_type AND `friend`=:friend_id AND follower=:follower_id",
                    "friend_type", viewer.type, "friend_id", viewer.id, "follower_id", targetIds[0]);
        } else if (targetIds.length > 1) {
            sql.where("`type`=:friend_type AND `friend`=:friend_id AND follower IN ($follower_ids)",
                    "friend_type", viewer.type, "friend_id", viewer.id, "follower_ids", StringHelper.join(targetIds, ","));
        } else {
            throw new IllegalArgumentException();
        }

        return sql.toString();
    }

}
