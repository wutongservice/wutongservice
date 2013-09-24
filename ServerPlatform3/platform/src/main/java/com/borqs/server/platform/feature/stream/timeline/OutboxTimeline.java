package com.borqs.server.platform.feature.stream.timeline;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.stream.PostFilter;

public abstract class OutboxTimeline extends StreamTimeline {

    protected OutboxTimeline() {
    }

    public abstract TimelineEntry lastIdBefore(Context ctx, PeopleId user, long max);

    public abstract TimelineEntries continuousGet(Context ctx, PeopleId user, PostFilter filter, int limit);

}
