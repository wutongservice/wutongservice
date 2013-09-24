package com.borqs.server.impl.friend;


import com.borqs.server.platform.feature.friend.*;
import com.borqs.server.platform.util.CollectionsHelper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

public class FriendRs {

    public static void readFriendEntries(ResultSet rs, AbstractFriendImpl.FriendEntries result) throws SQLException {
        while (rs.next()) {
            PeopleId friendId = new PeopleId(rs.getInt("type"), rs.getString("friend"));
            AbstractFriendImpl.FriendEntry fe = result.findFriend(friendId);
            if (fe == null) {
                fe = new AbstractFriendImpl.FriendEntry();
                fe.friendId = friendId;
                result.addFriend(fe);
            }
            fe.addCircle(rs.getInt("circle"), rs.getByte("reason"), rs.getLong("updated_time"));
        }
    }


    public static void fillCircles(ResultSet rs, AbstractFriendImpl.FriendEntries result, String loc) throws SQLException {
        Circles circles = new Circles();
        while (rs.next()) {
            circles.add(new Circle(rs.getInt("circle_id"), rs.getString("name"), rs.getLong("updated_time")));
        }
        for (int circleId : Circle.BUILTIN_ACTUAL_CIRCLES) {
            circles.add(new Circle(circleId, Circle.getBuiltinCircleName(circleId, loc), 0));
        }
        result.circles = circles;
    }

    public static int[] getCircleIds(ResultSet rs, int min) throws SQLException {
        ArrayList<Integer> circleIds = new ArrayList<Integer>();
        while (rs.next()) {
            int circleId = rs.getInt("circle_id");
            if (circleId >= min)
                circleIds.add(circleId);
        }
        return CollectionsHelper.toIntArray(circleIds);
    }

    public static long[] getFollowerIds(ResultSet rs) throws SQLException {
        ArrayList<Long> l = new ArrayList<Long>();
        while (rs.next())
            l.add(rs.getLong("follower"));
        return CollectionsHelper.toLongArray(l);
    }

    public static void fillRelationsInFriends(ResultSet rs, Relationships rels) throws SQLException {
        while (rs.next()) {
            PeopleId viewer = PeopleId.user(rs.getLong("user"));
            PeopleId target = new PeopleId(rs.getInt("type"), rs.getString("friend"));

            Relationship rel = rels.getRelation(viewer, target);
            if (rel != null)
                rel.addTargetInViewer(rs.getInt("circle"), rs.getInt("reason"), rs.getLong("updated_time"));
        }
    }

    public static void fillRelationsInFollowers(ResultSet rs, Relationships rels) throws SQLException {
        while (rs.next()) {
            PeopleId viewer = new PeopleId(rs.getInt("type"), rs.getString("friend"));
            PeopleId target = PeopleId.user(rs.getLong("follower"));

            Relationship rel = rels.getRelation(viewer, target);
            if (rel != null)
                rel.addViewerInTarget(rs.getInt("circle"), rs.getInt("reason"), rs.getLong("updated_time"));
        }
    }

    public static void fillRemarks(ResultSet rs, Map<PeopleId, String> remarks) throws SQLException {
        while (rs.next()) {
            PeopleId friendId = new PeopleId(rs.getInt("type"), rs.getString("friend"));
            remarks.put(friendId, rs.getString("remark"));
        }
    }
}
