package com.borqs.server.platform.feature.friend;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.logic.Logic;

import java.util.Map;

public interface FriendLogic extends Logic {

    Circle createCustomCircle(Context ctx, String circleName);
    boolean destroyCustomCircle(Context ctx, int circleId);
    boolean updateCustomCircleName(Context ctx, int circleId, String circleName);
    Circles getCircles(Context ctx, long userId, int[] circleIds, boolean withUsers);
    boolean hasCircle(Context ctx, long userId, int circleId);
    boolean hasAllCircles(Context ctx, long userId, int... circleIds);
    boolean hasAnyCircles(Context ctx, long userId, int... circleIds);

    void setFriendIntoCircles(Context ctx, int reason, PeopleId friendId, int... circleIds);
    void addFriendsIntoCircle(Context ctx, int reason, PeopleIds friendIds, int circleId);
    void removeFriendsInCircle(Context ctx, PeopleIds friendIds, int circleId);

    PeopleIds getFriendsInCircles(Context ctx, long userId, int... circleIds);
    PeopleIds getFriends(Context ctx, long userId);
    int getFriendCountInCircles(Context ctx, long userId, int... circleIds);
    int getFriendCount(Context ctx, long userId);
    Map<Long, Integer> getFriendsCounts(Context ctx, long... userIds);
    boolean hasFriendInCircles(Context ctx, long userId, int[] circleIds, PeopleId friendId);
    boolean hasAllFriendsInCircles(Context ctx, long userId, int[] circleIds, PeopleId... friendIds);
    boolean hasAnyFriendsInCircles(Context ctx, long userId, int[] circleIds, PeopleId... friendIds);
    boolean hasFriend(Context ctx, long userId, PeopleId friendId);
    boolean hasAllFriends(Context ctx, long userId, PeopleId... friendIds);
    boolean hasAnyFriends(Context ctx, long userId, PeopleId... friendIds);

    long[] getFollowers(Context ctx, PeopleId friendId, Page page);
    int getFollowersCount(Context ctx, PeopleId friendId);
    Map<PeopleId, Integer> getFollowersCounts(Context ctx, PeopleId... friendIds);

    Relationship getRelationship(Context ctx, PeopleId viewer, PeopleId target);
    Relationships getRelationships(Context ctx, PeopleId viewer, PeopleId... targets);

    void setRemark(Context ctx, PeopleId friendId, String remark);
    String getRemark(Context ctx, long userId, PeopleId friendId);
    Map<PeopleId, String> getRemarks(Context ctx, long userId, PeopleId... friendIds);

    // wrap
    long[] getBorqsFriendIdsInCircles(Context ctx, long userId, int... circleIds);
    Users getBorqsFriendsInCircles(Context ctx, long userId, int[] circleIds, String[] expCols);
    long[] getBorqsFriendIds(Context ctx, long userId);
    Users getBorqsFriends(Context ctx, long userId, String[] expCols);
    Users getFollowerUsers(Context ctx, PeopleId friendId, String[] expCols, Page page);
}
