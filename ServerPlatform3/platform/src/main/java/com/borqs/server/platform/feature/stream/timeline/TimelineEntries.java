package com.borqs.server.platform.feature.stream.timeline;


import com.borqs.server.platform.util.CollectionsHelper;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TimelineEntries extends ArrayList<TimelineEntry> {
    public TimelineEntries() {
    }

    public TimelineEntries(int initialCapacity) {
        super(initialCapacity);
    }

    public TimelineEntries(Collection<? extends TimelineEntry> c) {
        super(c);
    }

    public long[] getPostIds() {
        ArrayList<Long> l = new ArrayList<Long>();
        for (TimelineEntry entry : this) {
            l.add(entry.postId);
        }
        return CollectionsHelper.toLongArray(l);
    }

    public void removeSpecific(Collection<Long> postIds) {
        if (CollectionUtils.isNotEmpty(postIds)) {
            ArrayList<TimelineEntry> l = new ArrayList<TimelineEntry>();
            for (TimelineEntry entry : this) {
                if (!postIds.contains(entry.postId))
                    l.add(entry);
            }
            clear();
            addAll(l);
        }
    }

    public static TimelineEntries fromBytes(List<byte[]> bytesList) {
        TimelineEntries te = new TimelineEntries(bytesList.size());
        if (CollectionUtils.isNotEmpty(bytesList)) {
            for (byte[] bytes : bytesList)
                te.add(TimelineEntry.fromBytes(bytes));
        }
        return te;
    }
}
