package com.borqs.server.platform.feature.stream.timeline;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.stream.PostFilter;


public class MemoryFriendsTimeline extends FriendsTimeline {

    public MemoryFriendsTimeline() {
    }

    @Override
    public void removeTimeline(Context ctx, PeopleId user) {
        // do nothing
    }

    @Override
    public void add(Context ctx, PeopleId user, TimelineEntry entry) {
        // do nothing
    }

    @Override
    public TimelineResult get(Context ctx, PeopleId user, PostFilter filter, Page page) {
        if (!user.isUser())
            return TimelineResult.newEmpty();

        filter = regulateFilter(filter);
        page = regulatePage(page);
        if (page.getBegin() >= limit)
            return TimelineResult.newEmpty();

        TimelineEntries timeline = aggregate(ctx, user, filter);
        TimelineEntries te = fullFilter(null, timeline, ctx, user, filter);
        return TimelineResult.split(te, page);
    }
}
