package com.borqs.server.impl.psuggest;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.friend.FriendsHook;
import com.borqs.server.platform.feature.psuggest.PeopleSuggestLogic;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;

import java.util.List;

public class AcceptedFriendsHook implements FriendsHook {

    private PeopleSuggestLogic psuggest;

    public AcceptedFriendsHook() {
    }

    public AcceptedFriendsHook(PeopleSuggestLogic psuggest) {
        this.psuggest = psuggest;
    }

    public PeopleSuggestLogic getPeopleSuggest() {
        return psuggest;
    }

    public void setPeopleSuggest(PeopleSuggestLogic psuggest) {
        this.psuggest = psuggest;
    }

    @Override
    public void before(Context ctx, List<Entry> data) {
        // do nothing
    }

    @Override
    public void after(Context ctx, List<Entry> data) {
        if (CollectionUtils.isEmpty(data))
            return;

        for (Entry entry : data) {
            if (entry != null && ArrayUtils.isNotEmpty(entry.circleIds))
                psuggest.accept(ctx, entry.friendId);
        }
    }
}
