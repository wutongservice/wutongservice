package com.borqs.server.wutong.stream;

import com.borqs.server.base.ResponseError;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

import java.util.List;
import java.util.Map;

@SuppressWarnings("all")
public interface StreamLogic {
    boolean savePost(Context ctx,Record post);

    boolean disablePosts(Context ctx,String userId, List<String> postIds);

    Record findPost(Context ctx,String postId, List<String> cols);

    Record findPostTemp(Context ctx,String postId, List<String> cols);

    RecordSet findWhoSharedApp(Context ctx,String packageName, int limit);

    RecordSet findWhoRetweetStream(Context ctx,String target, int limit);

    RecordSet findPosts(Context ctx,List<String> postIds, List<String> cols);


    boolean updatePost0(Context ctx,String userId, String postId, Record post);

    String sinceAndMaxSQL(Context ctx,Map<String, String> alias, long since, long max);


    RecordSet selectPosts(Context ctx,String sql);


    RecordSet getUsersPosts01(Context ctx,String viewerId, List<String> userIds, List<String> circleIds, List<String> cols, long since, long max, int type, String appId, int page, int count);

    List<String> findGroupsFromUserIds(Context ctx,List<String> userIds);

    boolean hasRight(Context ctx,long groupId, long member, int minRole);

    boolean isGroupStreamPublic(Context ctx,long groupId);

    RecordSet getUsersPosts0(Context ctx,String viewerId, List<String> userIds, List<String> circleIds, List<String> cols, long since, long max, int type, String appId, int page, int count);

    RecordSet getPostsNearBy0(Context ctx,String viewerId, String cols, long since, long max, int type, String appId, int page, int count);

    int getSharedCount(Context ctx,String viewerId, String userId, int type);

    String isTarget(Context ctx,String viewerId);

    RecordSet getMySharePosts0(Context ctx,String viewerId, List<String> userIds, List<String> cols, long since, long max, int type, String appId, int page, int count);

    boolean updateAttachment(Context ctx,String post_id, String Attachments);

    boolean updatePostFor0(Context ctx,String post_id, String newPost_id, String Attachments, long created_time, long updated_time);

    boolean updatePostForAttachmentsAndUpdateTime(Context ctx,String post_id, String Attachments, long updated_time);

    boolean updatePostForCommentOrLike(Context ctx,String post_id, String viewerId, String column, int value);

    RecordSet topOneStreamByTarget0(Context ctx,int type, String target);

    RecordSet topOneStreamBySetFriend0(Context ctx,int type, String source, long created_time);

    RecordSet topOneStreamByShare(Context ctx,int type, String source, String message, String mentions, int privince, long dateDiff);

    RecordSet myTopOneStreamByTarget0(Context ctx,String userId, int type, String target, List<String> cols);

    boolean touch(Context ctx,String postId);

    RecordSet topSendStreamUser(Context ctx,int limit);

    RecordSet getApkSharedToMe(Context ctx,String viewerId, String userIds, boolean tome, String packageName, int page, int count);

    RecordSet getSharedByType(Context ctx,String userIds, int type, String cols, int page, int count);

    RecordSet getSharedPost(Context ctx,String viewerId, String postId);

    RecordSet getSharedPostHasContact1(Context ctx,String contact);


    RecordSet getSharedPostHasContact2(Context ctx,String virtual_friendId);


    boolean updatePostHasContact2(Context ctx,String postId, String newMentions, String newAddContact, boolean newHasContact);


    RecordSet formatOldDataToConversation0(Context ctx,String viewerId);

    boolean ifExistConversation0(Context ctx,int target_type, String target_id, int reason, long from);


    boolean formatLocation(Context ctx,String sql);

    boolean getPhoto(Context ctx,String viewerId, String photo_id);

    boolean getFile(Context ctx,String viewerId, String file_id);

    Record getVideo(Context ctx,String viewerId, String file_id);

    Record getAudio0(Context ctx,String viewerId, String file_id);

    Record getStaticFile(Context ctx, String viewerId, String file_id);

    RecordSet getAppliesToUser(Context ctx, String viewerId, String appId, String userId, String cols);


    String createPost(Context ctx,String userId, Record post0);

    Record destroyPosts(Context ctx, String userId, String postIds);

    Record findStreamTemp(Context ctx,String postId, String cols);

    RecordSet getPostsNearBy(Context ctx, String viewerId, String cols, long since, long max, int type, String appId, int page, int count);

    RecordSet topOneStreamByTarget(Context ctx,int type, String target);

    String createRepost(Context ctx,String userId, String mentions, boolean secretly, String postId, String message, String device, String location, String appData, boolean can_comment, boolean can_like, boolean can_reshare, String add_to, String add_contact, boolean has_contact);

    boolean updatePost(Context ctx,String userId, String postId, Record post);

    RecordSet getPosts(Context ctx,String postIds, String cols);

    boolean hasPost(Context ctx,String postId);


    RecordSet getUsersPosts(Context ctx,String viewerId, String userIds, String circleIds, String cols, long since, long max, int type, String appId, int page, int count);

    RecordSet getMySharePosts(Context ctx,String viewerId, String userIds, String cols, long since, long max, int type, String appId, int page, int count);


    RecordSet selectPostsBySql(Context ctx,String sql);


    RecordSet myTopOneStreamByTarget(Context ctx,String userId, int type, String target, String cols);


    RecordSet topOneStreamBySetFriend(Context ctx,int type, String source, long created_time);

    String updatePostFor(Context ctx,String post_id, String Attachments, long created_time, long updated_time);

    boolean postCanCommentP(Context ctx, String postId);

    Record getPostP(Context ctx, String postId, String cols);

    boolean postCanLikeP(Context ctx, String postId);

    RecordSet getNearByStreamP(Context ctx, String viewerId, String cols, long since, long max, int type, String appId, int page, int count, String location, int dis) throws ResponseError;

    RecordSet transTimelineForQiupuP(Context ctx, String viewerId, RecordSet reds, int getCommentCount, int getLikeUsers, boolean single_get);

    RecordSet getFriendsTimelineP(Context ctx, String userId, String circleIds, String cols, long since, long max, int type, String appId, int page, int count);

    RecordSet getFriendsTimelineForQiuPuP(Context ctx, String viewerId, String userId, String circleIds, String cols, long since, long max, int type, String appId, int page, int count);

    RecordSet getFullFriendsTimelineForQiuPuP(Context ctx, String viewerId, String userId, String circleIds, long since, long max, int type, String appId, int page, int count);

    RecordSet getFullFriendsTimelineP(Context ctx, String userId, String circleIds, long since, long max, int type, String appId, int page, int count);

    RecordSet getMyShareFullTimelineP(Context ctx, String viewerId, String userIds, long since, long max, int type, String appId, int page, int count);

    RecordSet getMyShareTimelineP(Context ctx, String viewerId, String userIds, String cols, long since, long max, int type, String appId, int page, int count);

    RecordSet getMyShareP(Context ctx, String viewerId, String userIds, String cols, long since, long max, int type, String appId, int page, int count);

    RecordSet getFullUsersTimelineForQiuPuP(Context ctx, String viewerId, String userIds, long since, long max, int type, String appId, int page, int count);

    RecordSet getFullUsersTimelineP(Context ctx, String viewerId, String userIds, long since, long max, int type, String appId, int page, int count);

    RecordSet getUsersTimelineForQiuPuP(Context ctx, String viewerId, String userIds, String cols, long since, long max, int type, String appId, int page, int count);

    RecordSet getUsersTimelineP(Context ctx, String viewerId, String userIds, String cols, long since, long max, int type, String appId, int page, int count);

    RecordSet getPublicTimelineP(Context ctx, String viewerId, String cols, long since, long max, int type, String appId, int page, int count);

    RecordSet getFullPublicTimelineP(Context ctx, String viewerId, long since, long max, int type, String appId, int page, int count);

    RecordSet getPublicTimelineForQiuPuP(Context ctx, String viewerId, String cols, long since, long max, int type, String appId, int page, int count);

    RecordSet getFullPublicTimelineForQiuPuP(Context ctx, String viewerId, long since, long max, int type, String appId, int page, int count);

    RecordSet getHotStream(Context ctx, String viewerId, String circle_ids, String cols, int type, long max, long min, int page, int count);

    Record findStreamTempP(Context ctx, String postId, String cols);

    RecordSet getPostsForQiuPuP(Context ctx, String viewerId, String postsIds, String cols, boolean single_get);

    RecordSet getFullPostsForQiuPuP(Context ctx, String viewerId, String postIds, boolean single_get);

    boolean updateStreamCanCommentOrcanLike(Context ctx, String post_id, String viewerId, Record rec);

    boolean updatePostP(Context ctx, String userId, String postId, String message);

    String repostP(Context ctx, String userId, String mentions, boolean secretly, String postId, String newMessage, String device, String location, String appData, boolean can_comment, boolean can_like, boolean can_reshare, String add_to, boolean sendEmail, boolean sendSms, boolean isTop);

    boolean destroyPostsP(Context ctx, String userId, String postIds);

    Record sendShareLinkP(Context ctx, String userId, String msg, String appId, String mentions, String app_data,
                          boolean secretly, String device, String location, String url, String title, String linkImagAddr, boolean can_comment, boolean can_like, boolean can_reshare, String add_to, boolean sendEmail, boolean sendSms, boolean isTop, long scene);

    Record postP(Context ctx, String userId, Record post, List<String> emails, List<String> phones, String appId, boolean sendEmail, boolean sendSms, boolean isTop);

    void autoPost(Context ctx, String userId, int type, String msg, String attachments,
                  String appId, String packageName, String apkId,
                  String appData,
                  String mentions,
                  boolean secretly, String cols, String device, String location,
                  boolean can_comment, boolean can_like, boolean can_reshare, String add_to, String add_contact, boolean has_contact,int post_source);

    Record postP(Context ctx, String userId, int type, String msg, String attachments,
                 String appId, String packageName, String apkId,
                 String appData,
                 String mentions,
                 boolean secretly, String cols, String device, String location,
                 String url, String linkImagAddr,
                 boolean can_comment, boolean can_like, boolean can_reshare,
                 String add_to, boolean sendEmail, boolean sendSms, boolean isTop,int post_source, long scene);

    Record postP(Context ctx, String userId, int type, String msg, String attachments,
                 String appId, String packageName, String apkId,
                 String appData,
                 String mentions,
                 boolean secretly, String cols, String device, String location,
                 String url, String linkImagAddr,
                 boolean can_comment, boolean can_like, boolean can_reshare,
                 String add_to,int post_source, long scene);

    RecordSet getPostsP(Context ctx,String postIds, String cols);

    public void sendPostBySetFriend(Context ctx, String userId, String friendIds, int reason, boolean can_comment, boolean can_like, boolean can_reshare,int post_source);
    public boolean sendPostBySetFriend0(Context ctx, String userId, String friendIds, int reason, boolean can_comment, boolean can_like, boolean can_reshare,int post_source);

    RecordSet getSharedCountAll(Context ctx, String viewerId, String userId);
    int updatePhotoOld(Context ctx);

    RecordSet getRelatedPosts0(Context ctx, String userId, List<String> cols, long since, long max, int type, String appId, int page, int count, List<String> groupIds);
    RecordSet getRelatedPosts(Context ctx, String userId, String cols, long since, long max, int type, String appId, int page, int count, List<String> groupIds);
    RecordSet getRelatedTimelineP(Context ctx, String userId, String cols, long since, long max, int type, String appId, int page, int count, List<String> groupIds);
    RecordSet getRelatedTimelineForQiupuP(Context ctx, String userId, String cols, long since, long max, int type, String appId, int page, int count, List<String> groupIds);
    RecordSet getFullRelatedTimelineForQiupuP(Context ctx, String userId, long since, long max, int type, String appId, int page, int count, List<String> groupIds);
}