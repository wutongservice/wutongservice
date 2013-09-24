package com.borqs.server.platform.feature.stream.timeline;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.stream.PostFilter;

import java.util.LinkedList;

public class MemoryOutboxTimeline extends OutboxTimeline {

    private final MemoryTimelines timelines = new MemoryTimelines();

    public MemoryOutboxTimeline() {
    }

    @Override
    public TimelineEntry lastIdBefore(Context ctx, PeopleId user, long max) {
        TimelineEntries te = timelines.getTimeline(user);
        for (TimelineEntry entry : te) {
            if (entry.postId <= max)
                return entry;
        }
        return null;
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

    @Override
    public TimelineEntries continuousGet(Context ctx, PeopleId user, PostFilter filter, int limit) {
        filter = regulateFilter(filter);

        TimelineEntries all = timelines.getTimeline(user);
        TimelineEntries timeline = new TimelineEntries();
        final int FETCH_COUNT = 10;
        for (int begin = 0; ; begin += FETCH_COUNT) {
            int end = begin + FETCH_COUNT;
            if (end > all.size())
                end = all.size();

            for (TimelineEntry entry : all.subList(begin, end)) {
                if (filterEntry(entry, ctx, user, FILTER_COLUMNS, filter)) {
                    timeline.add(entry);
                    if (timeline.size() >= limit)
                        break;
                }
            }

            if (end >= all.size())
                break;
        }
        return timeline;
    }
}
