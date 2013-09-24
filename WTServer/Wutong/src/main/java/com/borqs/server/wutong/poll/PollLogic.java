package com.borqs.server.wutong.poll;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

public interface PollLogic {
    long createPoll(Context ctx, Record poll, RecordSet items);

    RecordSet getPolls(Context ctx, String pollIds);

    RecordSet getItemsByPollId(Context ctx, long pollId);

    boolean deletePolls(Context ctx, String viewerId, String pollIds);

    RecordSet getItemsByItemIds(Context ctx, String itemIds);

    boolean vote(Context ctx, String userId, long pollId, Record items);

    long hasVoted(Context ctx, String userId, long pollId);

    Record getCounts(Context ctx, String pollIds);

    String getCreatedPolls(Context ctx, String viewerId, String userId, int page, int count);

    String getParticipatedPolls(Context ctx, String viewerId, String userId, int page, int count);

    String getInvolvedPolls(Context ctx, String viewerId, String userId, int page, int count);

    String getFriendsPolls(Context ctx, String viewerId, String userId, int sort, int page, int count);

    String getPublicPolls(Context ctx, String viewerId, String userId, int sort, int page, int count);

    long getRelatedPollCount(Context ctx, String viewerId, String userId);

    long createPoll(Context ctx, Record poll, RecordSet items, String ua, String loc, String appId, boolean sendPost, boolean sendEmail, boolean sendSms) ;

    RecordSet getPolls(Context ctx, String viewerId, String pollIds, boolean withItems) ;

    boolean vote(Context ctx, String userId, long pollId, Record items, String ua, String loc, String appId, boolean sendPost) ;

    RecordSet getInvolvedPollsPlatform(Context ctx, String viewerId, String userId, int page, int count) ;

    RecordSet getParticipatedPollsPlatform(Context ctx, String viewerId, String userId, int page, int count) ;

    RecordSet getCreatedPollsPlatform(Context ctx, String viewerId, String userId, int page, int count) ;

    RecordSet getPublicPollsPlatform(Context ctx, String viewerId, String userId, int sort, int page, int count) ;

    RecordSet getFriendsPollsPlatform(Context ctx, String viewerId, String userId, int sort, int page, int count) ;

    boolean canVote(Context ctx, String viewerId, long pollId);

    boolean addItems(Context ctx, RecordSet items);
}