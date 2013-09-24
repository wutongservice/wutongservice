package com.borqs.server.wutong.friendship;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

public interface FriendshipLogic {
    boolean createBuiltinCircles(Context ctx,String userId);

    String createCircle(Context ctx,String userId, String name);

    boolean destroyCircles(Context ctx,String userId, String circleIds);

    boolean updateCircleName(Context ctx,String userId, String circleId, String name);

    boolean updateCircleMemberCount(Context ctx,String userId, String circleId, int member_count);

    boolean updateMyCircleMemberCount(Context ctx,String userId, String circleId);

    RecordSet getCircles(Context ctx,String userId, String circleIds, boolean withMembers);

    boolean setContactFriend(Context ctx,String userId, String friendId, String fname, String content, String circleIds, int reason, boolean isadd, boolean deleteOld);

    boolean getIfHeInMyCircles(Context ctx,String my_id, String other_id, String circle_id);

    boolean setFriend(Context ctx,String userId, String friendId, String circleIds, int reason);

    boolean setFriends(Context ctx,String userId, String friendId, String circleId, int reason, boolean isadd);

    RecordSet getFriends(Context ctx,String userId, String circleIds, int page, int count);

    RecordSet getFollowers(Context ctx,String userId, String circleIds, int page, int count);

    RecordSet getBothFriendsIds(Context ctx,String viewerId, String userId, int page, int count);

    RecordSet getRelation(Context ctx,String sourceUserId, String targetUserId, String circleId);

    Record getBidiRelation(Context ctx,String sourceUserId, String targetUserId, String circleId);

    boolean setRemark(Context ctx,String userId, String friendId, String remark);

    RecordSet getRemarks(Context ctx,String userId, String friendIds);

    Record isDeleteRecent(Context ctx,String userId, String friendIds, long period);

    RecordSet getFriendOrFollowers(Context ctx,String userIds, String byFriendOrFollowers);

    RecordSet getAllRelation(Context ctx,String viewerId, String userIds, String circleId, String inTheirOrInMine);

    RecordSet topUserFollowers(Context ctx, long userId, int limit);

    Record getMyFriends(Context ctx,String userId, String friendId);

    int getFollowersCount(Context ctx,String userId);

    int getFriendsCount(Context ctx,String userId);

    boolean createVirtualFriendId(Context ctx,String userId, String friendId, String content, String name);

    boolean updateVirtualFriendIdToAct(Context ctx,String friendId, String content);

    RecordSet getContactFriend(Context ctx,String userIds);

    RecordSet getContactFriendByFid(Context ctx,String friendIds);

    RecordSet getVirtualFriendId(Context ctx,String content);

    String getUserFriendHasVirtualFriendId(Context ctx, String userId, String content);

    RecordSet getVirtualFriendIdByName(Context ctx,String userId, String name);

    boolean followPage(Context ctx, long viewerId, long pageId);

    void unfollowPage(Context ctx, long viewerId, long pageId);

    long[] getFollowedPageIds(Context ctx, long viewerId);

    long[] isFollowedPages(Context ctx, long viewerId, long[] pageIds);

    long[] getFollowerIds(Context ctx, long viewerId, long idBegin, long idEnd, int page, int count);

    // Platform method
    boolean destroyCircleP(Context ctx, String userId, String circleIds);

    RecordSet getCirclesP(Context ctx, String userId, String circleIds, boolean withUsers);

    boolean setFriendsP(Context ctx, String userId, String friendIds, String circleId, int reason, boolean isadd);

    boolean isHisFriendP(Context ctx, String sourceUserId, String targetUserId);

    boolean isFriendP(Context ctx, String sourceUserId, String targetUserId);

    RecordSet getRelationP(Context ctx, String sourceUserId, String targetUserId);

    String setContactFriendP(Context ctx, String userId, String friendName, String content, String circleIds, int reason);

    Record setFriendP(Context ctx, String userId, String friendId, String circleIds, int reason);

    Record exchangeVcardP(Context ctx, String userId, String friendId, String circleIds, int reason, boolean send_request);

    RecordSet dealWithInCirclesByGroupsP(Context ctx, long begin, long end, String userId, String friendId, RecordSet reuse);

    RecordSet getFriendsP(Context ctx, String viewerId, String userId, String circleIds, String cols, boolean inPublicCircles, int page, int count);

    RecordSet getFriendsV2P(Context ctx, String viewerId, String userId, String circleIds, String cols, int page, int count);

    RecordSet getBothFriendsP(Context ctx, String viewerId, String userId, int page, int count);

    RecordSet getFollowersP(Context ctx, String viewerId, String userId, String circleIds, String cols, int page, int count);

    RecordSet getFriendsP(Context ctx, String viewerId, String userId, String circleIds, String cols, int page, int count);

    String getFriendsIdP(Context ctx, String userId, String circleIds, String cols, int page, int count);
}