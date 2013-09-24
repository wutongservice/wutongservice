package com.borqs.server.platform.feature.stream.timeline;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.stream.PostFilter;
import com.borqs.server.platform.util.CollectionsHelper;
import org.apache.commons.collections.CollectionUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public abstract class FriendsTimeline extends StreamTimeline {
    public static final int DEFAULT_LIMIT = 800;


    protected OutboxTimeline outboxTimeline;
    protected volatile int limit = DEFAULT_LIMIT;

    protected FriendsTimeline() {
    }

    public OutboxTimeline getOutboxTimeline() {
        return outboxTimeline;
    }

    public void setOutboxTimeline(OutboxTimeline outboxTimeline) {
        this.outboxTimeline = outboxTimeline;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public TimelineEntries aggregateCount(Context ctx, PeopleId user, PostFilter filter, int count) {
        if (CollectionUtils.isEmpty(filter.friendIds))
            return new TimelineEntries();

        HashMap<PeopleId, TimelineEntry> tem = new HashMap<PeopleId, TimelineEntry>();
        for (PeopleId friendId : filter.friendIds) {
            TimelineEntry entry = outboxTimeline.lastIdBefore(ctx, friendId, filter.max);
            if (entry != null)
                tem.put(friendId, entry);
        }
        if (tem.isEmpty())
            return new TimelineEntries();

        int fetchCount = calcFetchCount(count, tem.size());

        TreeSet<TimelineEntryFetcher> sorted = new TreeSet<TimelineEntryFetcher>(TIMELINE_ENTRY_FETCHER_COMPARATOR);
        for (Map.Entry<PeopleId, TimelineEntry> e : tem.entrySet())
            sorted.add(new TimelineEntryFetcher(e.getKey(), filter, e.getValue(), fetchCount));

        TimelineEntries timeline = new TimelineEntries();
        for (int i = 0; i < count; i++) {
            TimelineEntryFetcher fetcher = sorted.pollLast();
            TimelineEntry entry = fetcher.fetchOne(ctx);
            if (entry != null) {
                timeline.add(entry);
                sorted.add(fetcher);
            }

            if (sorted.isEmpty())
                break;
        }

        CollectionsHelper.trimSize(timeline, count);
        return timeline;
    }

    public TimelineEntries aggregate(Context ctx, PeopleId user, PostFilter filter) {
        return aggregateCount(ctx, user, filter, limit);
    }

    protected int calcFetchCount(int count, int friendCount) {
        double d =  (double)count / friendCount;
        int n = (int)d + 1;
        if (n < 20)
            n = 20;
        return n;
    }

    protected class TimelineEntryFetcher {
        private int position;
        private PeopleId user;
        private PostFilter filter;
        private TimelineEntries subTimeline;
        private int fetchCount;

        public TimelineEntryFetcher(PeopleId user, PostFilter filter, TimelineEntry beginEntry, int fetchCount) {
            this.user = user;
            this.filter = filter;
            subTimeline = new TimelineEntries(1);
            subTimeline.add(beginEntry);
            position = 0;
            this.fetchCount = fetchCount;
        }

        public long currentId() {
            if (CollectionUtils.isEmpty(subTimeline))
                return 0;

            TimelineEntry entry = position >= 0 ? subTimeline.get(position) : null;
            return entry != null ? entry.postId : 0;
        }

        private TimelineEntry last() {
            return subTimeline.get(subTimeline.size() - 1);
        }

        public TimelineEntry fetchOne(Context ctx) {
            if (CollectionUtils.isEmpty(subTimeline))
                return null;

            TimelineEntry entry = subTimeline.get(position);
            position++;
            if (position >= subTimeline.size()) {
                PostFilter newFilter = filter.copyWithMax(last().postId - 1);
                subTimeline = outboxTimeline.continuousGet(ctx, user, newFilter, fetchCount);
                position = 0;
            }
            return entry;
        }
    }

    protected static Comparator<TimelineEntryFetcher> TIMELINE_ENTRY_FETCHER_COMPARATOR = new Comparator<TimelineEntryFetcher>() {
        @Override
        public int compare(TimelineEntryFetcher o1, TimelineEntryFetcher o2) {
            if (o1 == o2)
                return 0;

            if (o1 == null)
                return -1;

            if (o2 == null)
                return 1;

            long postId1 = o1.currentId();
            long postId2 = o2.currentId();
            return postId1 > postId2 ? 1 : (postId1 < postId2 ? -1 : 0);
        }
    };
}
