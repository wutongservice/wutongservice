package com.borqs.server.platform.feature.stream.timeline;


import com.borqs.server.platform.data.Page;

import java.util.List;

public class TimelineResult  {

    public final TimelineEntries timeline;
    public final int total;

    public TimelineResult(TimelineEntries timeline, int total) {
        this.timeline = timeline;
        this.total = total;
    }

    public static TimelineResult split(List<TimelineEntry> te, Page page) {
        TimelineEntries r = new TimelineEntries();
        page.retainsTo(te, r);
        return new TimelineResult(r, te.size());
    }

    public static TimelineResult newEmpty() {
        return new TimelineResult(new TimelineEntries(), 0);
    }
}
