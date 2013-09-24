package com.borqs.server.compatible;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.psuggest.PeopleSuggests;
import com.borqs.server.platform.feature.psuggest.SuggestionReasons;
import org.apache.commons.collections.CollectionUtils;

public class CompatiblePeopleSuggest {
    public static final String V1COL_SUGGEST_TYPE = "suggest_type";
    public static final String V1COL_SUGGEST_REASON = "suggest_reason";

    public static Users getSuggestUsers(PeopleSuggests pss, Context ctx, AccountLogic account, String[] v1SuggestedCols, String[] v1SourceCols) {
        String[] suggestedCols = CompatibleUser.v1ToV2Columns(v1SuggestedCols);
        String[] sourceCols = CompatibleUser.v1ToV2Columns(v1SourceCols);

        if (CollectionUtils.isEmpty(pss))
            return new Users();

        long[] userIds = pss.getSuggesteds().getUserIds();
        Users users = account.getUsers(ctx, suggestedCols, userIds);
        if (users.isEmpty())
            return users;

        long[] allSourceIds = pss.getRecommendingSourceIds(users.getPeopleIds(null).toIdArray());
        Users allSourceUsers = account.getUsers(ctx, sourceCols, allSourceIds);
        for (User user : users) {
            // V1COL_SUGGEST_TYPE
            int foremostReason = pss.getForemostReason(user.getPeopleId());
            user.setAddon(V1COL_SUGGEST_TYPE, Integer.toString(foremostReason));

            // V1COL_SUGGEST_REASON
            Users sourceUsers = new Users();
            if (foremostReason == SuggestionReasons.RECOMMENDER_USER) {
                long[] sourceIds = pss.getRecommendingSourceIds(user.getPeopleId());
                allSourceUsers.getUsers(sourceUsers, sourceIds);
            }
            user.setAddon(V1COL_SUGGEST_REASON, Addons.jsonAddonValue(CompatibleUser.usersToJson(sourceUsers, v1SourceCols, true)));
        }
        return users;
    }
}
