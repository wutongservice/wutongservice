package com.borqs.server.wutong.usersugg;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.RecordSet;

import java.util.List;
import java.util.Map;

public interface SuggestedUserLogic {
    boolean refuseSuggestUser(Context ctx, String userId, String suggested);

    boolean deleteSuggestUser(Context ctx, String userId, String suggested);

    RecordSet getSuggestUser(Context ctx, String userId, int limit);

    RecordSet getIfExistSuggestUser(Context ctx, String userId, String suggestUserId);

    boolean createSuggestUser(Context ctx, String userId, String suggestedUsers, int type, String reason);

    boolean updateSuggestUser(Context ctx, String userId, String suggestedUsers, int type, String reason);

    String getWhoSuggest(Context ctx, String to, String beSuggested);

    boolean backSuggestUser(Context ctx, String userId, long dateDiff);

    RecordSet getSuggestFromBothFriend(Context ctx, String userId);

    RecordSet getSuggestFromHasMyContactInfo(Context ctx, String userId);

    RecordSet getSuggestUserHistory(Context ctx, String userId, int limit);

    RecordSet getWhoSuggestedHim(Context ctx, String userId, String beSuggested);


    boolean refuseSuggestUserP(Context ctx, String userId, String suggested);

    boolean createSuggestUserP(Context ctx, String userId, String suggestedUsers, int type, String reason);

    boolean deleteSuggestUserP(Context ctx, String userId, String suggested);

    boolean backSuggestUserP(Context ctx, String userId);

    boolean createSuggestUserFromHaveBorqsIdP(Context ctx, String userId);

    boolean createSuggestUserFromHaveCommLXRP(Context ctx, String userId);

    boolean createSuggestUserByHasMyContactP(Context ctx, String userId);

    boolean createSuggestUserFromCommonFriendsP(Context ctx, String userId);

    boolean autoCreateSuggestUsersP(Context ctx, String userId);

    RecordSet getSuggestUserP(Context ctx, String userId, int limit, boolean getBack);

    boolean updateSuggestUserReasonP(Context ctx);

    boolean recommendUserP(Context ctx, String whoSuggest, String toUserId, String beSuggestedUserIds);

    boolean createSuggestUserFromSameSchoolP(Context ctx, String userId, Map<String, List<String>> map);

    boolean createSuggestUserFromSameCompanyP(Context ctx, String userId, Map<String, List<String>> map);
}