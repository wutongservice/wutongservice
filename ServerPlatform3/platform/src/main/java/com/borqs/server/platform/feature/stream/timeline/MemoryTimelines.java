package com.borqs.server.platform.feature.stream.timeline;


import com.borqs.server.platform.feature.friend.PeopleId;
import org.apache.commons.collections.CollectionUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MemoryTimelines {
    private final ReadWriteLock RWL = new ReentrantReadWriteLock();
    private final Map<PeopleId, LinkedList<TimelineEntry>> timelines = new HashMap<PeopleId, LinkedList<TimelineEntry>>();

    public MemoryTimelines() {
    }

    public boolean hasTimeline(PeopleId user) {
        return getTimeline(user) != null;
    }

    public void removeTimeline(PeopleId user) {
        timelines.remove(user);
    }

    public TimelineEntries getTimeline(PeopleId user) {
        try {
            RWL.readLock().lock();
            LinkedList<TimelineEntry> timeline = timelines.get(user);
            return CollectionUtils.isEmpty(timeline) ? new TimelineEntries() : new TimelineEntries(timeline);
        } finally {
            RWL.readLock().unlock();
        }
    }

    public LinkedList<TimelineEntry> ensureTimeline(PeopleId user) {
        try {
            RWL.writeLock().lock();
            LinkedList<TimelineEntry> timeline = timelines.get(user);
            if (timeline == null) {
                timeline = new LinkedList<TimelineEntry>();
                timelines.put(user, timeline);
            }
            return timeline;
        } finally {
            RWL.writeLock().unlock();
        }
    }
}
