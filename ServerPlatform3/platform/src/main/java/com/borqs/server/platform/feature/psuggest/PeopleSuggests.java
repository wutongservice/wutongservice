package com.borqs.server.platform.feature.psuggest;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;

import java.util.*;

public class PeopleSuggests extends ArrayList<PeopleSuggest> {

    public static final String COL_SUGGESTED_REASON = "suggested_reason";
    public static final String COL_RECOMMEND_BY = "recommend_by";


    public PeopleSuggests(int initialCapacity) {
        super(initialCapacity);
    }

    public PeopleSuggests() {
    }

    public PeopleSuggests(Collection<? extends PeopleSuggest> c) {
        super(c);
    }


    public long[] getRecommendingSourceIds(PeopleId... suggestedIds) {
        if (ArrayUtils.isEmpty(suggestedIds))
            return new long[0];

        LinkedHashSet<Long> set = new LinkedHashSet<Long>();
        for (PeopleId suggestedId : suggestedIds) {
            for (PeopleSuggest ps : this) {
                if (ps != null && suggestedId.equals(ps.getSuggested()) && ps.getReason() == SuggestionReasons.RECOMMENDER_USER) {
                    for (long id : ps.getSourceAsLongArray())
                        set.add(id);
                }
            }
        }
        return CollectionsHelper.toLongArray(set);
    }

    public int getForemostReason(PeopleId suggestedId) {
        if (suggestedId == null)
            return SuggestionReasons.REASON_NONE;

        int foremostReason = Integer.MAX_VALUE;
        for (PeopleSuggest ps : this) {
            if (ps != null && suggestedId.equals(ps.getSuggested())) {
                if (ps.getReason() < foremostReason)
                    foremostReason = ps.getReason();
            }
        }
        if (foremostReason == Integer.MAX_VALUE)
            return SuggestionReasons.REASON_NONE;

        return foremostReason > 0 ? foremostReason : SuggestionReasons.REASON_NONE;
    }


    public Map<PeopleId, Map<Integer, long[]>> getGroupedSources() {
        LinkedHashMap<PeopleId, Map<Integer, long[]>> map = new LinkedHashMap<PeopleId, Map<Integer, long[]>>();
        for (PeopleSuggest suggest : this) {
            PeopleId friendId = suggest.getSuggested();
            int reason = suggest.getReason();
            long[] source = StringHelper.splitLongArray(suggest.getSource(), ",");
            Map<Integer, long[]> m = new LinkedHashMap<Integer, long[]>();
            if (map.containsKey(friendId))
                m = map.get(friendId);
            m.put(reason, source);
            map.put(friendId, m);
        }
        return map;
    }


    public PeopleIds getSuggesteds() {
        PeopleIds friendIds = new PeopleIds();
        for (PeopleSuggest suggest : this) {
            PeopleId friendId = suggest.getSuggested();
            if (!friendIds.contains(friendId))
                friendIds.add(friendId);
        }
        return friendIds;
    }

    public Map<Long, PeopleIds> getGroupedSuggesteds() {
        LinkedHashMap<Long, PeopleIds> m = new LinkedHashMap<Long, PeopleIds>();
        for (PeopleSuggest suggest : this) {
            PeopleId friendId = suggest.getSuggested();
            long[] source = StringHelper.splitLongArray(suggest.getSource(), ",");
            for (long src : source) {
                PeopleIds friendIds = new PeopleIds();
                if (m.containsKey(src))
                    friendIds.addAll(m.get(src));
                Collections.addAll(friendIds, friendId);
                m.put(src, friendIds);
            }
        }
        return m;
    }


    public Users getSuggestedUsers(Context ctx, AccountLogic account, String[] suggestedCols, String[] sourceCols) {
        if (isEmpty())
            return new Users();

        long[] userIds = getSuggesteds().getUserIds();
        Users users = account.getUsers(ctx, suggestedCols, userIds);
        if (users.isEmpty())
            return users;

        long[] allSourceIds = getRecommendingSourceIds(users.getPeopleIds(null).toIdArray());
        Users allSourceUsers = account.getUsers(ctx, sourceCols, allSourceIds);
        for (User user : users) {
            // COL_SUGGESTED_REASON
            int foremostReason = getForemostReason(user.getPeopleId());
            user.setAddon(COL_SUGGESTED_REASON, foremostReason);

            // COL_RECOMMEND_BY
            Users sourceUsers = new Users();
            if (foremostReason == SuggestionReasons.RECOMMENDER_USER) {
                long[] sourceIds = getRecommendingSourceIds(user.getPeopleId());
                allSourceUsers.getUsers(sourceUsers, sourceIds);
            }
            user.setAddon(COL_RECOMMEND_BY, Addons.jsonAddonValue(sourceUsers.toJson(sourceCols, true)));
        }
        return users;
    }
}
