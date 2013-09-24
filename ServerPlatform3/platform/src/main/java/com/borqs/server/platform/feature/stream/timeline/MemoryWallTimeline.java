package com.borqs.server.platform.feature.stream.timeline;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.stream.PostFilter;

import java.util.LinkedList;


public class MemoryWallTimeline extends WallTimeline {

    private final MemoryTimelines timelines = new MemoryTimelines();

    public MemoryWallTimeline() {
    }

    @Override
    public void removeTimeline(Context ctx, PeopleId user) {
        timelines.removeTimeline(user);
    }

    @Override
    public void add(Context ctx, PeopleId user, TimelineEntry entry) {
        LinkedList<TimelineEntry> timeline = timelines.ensureTimeline(user);
        timeline.addFirst(entry.copy());
    }

    @Override
    public TimelineResult get(Context ctx, PeopleId user, PostFilter filter, Page page) {
        filter = regulateFilter(filter);
        page = regulatePage(page);

        TimelineEntries timeline = timelines.getTimeline(user);
        TimelineEntries te = fullFilter(null, timeline, ctx, user, filter);
        return TimelineResult.split(te, page);
    }
}
