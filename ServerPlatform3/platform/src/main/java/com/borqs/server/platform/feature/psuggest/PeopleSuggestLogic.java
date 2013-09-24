package com.borqs.server.platform.feature.psuggest;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.logic.Logic;

public interface PeopleSuggestLogic extends Logic {
    void create(Context ctx, PeopleSuggest... suggests);
    void accept(Context ctx, PeopleId... suggested);
    void reject(Context ctx, PeopleId... suggested);

    PeopleSuggests getSuggested(Context ctx, long userId, int limit);
    PeopleSuggests getAccepted(Context ctx, long userId);
    PeopleSuggests getRejected(Context ctx, long userId);
    PeopleSuggests getPeopleSource(Context ctx, long userId,long id);
}
