package com.borqs.server.impl.stream.timeline;


import com.borqs.server.platform.cache.redis.Redis;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.app.App;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.PostFilter;
import com.borqs.server.platform.feature.stream.timeline.OutboxTimeline;
import com.borqs.server.platform.feature.stream.timeline.TimelineEntries;
import com.borqs.server.platform.feature.stream.timeline.TimelineEntry;
import com.borqs.server.platform.feature.stream.timeline.TimelineResult;
import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.util.CollectionsHelper;
import org.apache.commons.collections.CollectionUtils;
import redis.clients.jedis.BinaryJedisCommands;

import java.util.List;

public class RedisOutboxTimeline extends OutboxTimeline {
    public static final int MAX_ENTRY_COUNT = 50000;

    private Redis redis;

    public RedisOutboxTimeline() {
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    private static byte[] makeKey(PeopleId user) {
        return Charsets.toBytes("o-" + user.toStringId());
    }

    public int getCount(final Context ctx, final PeopleId user) {
        final byte[] key = makeKey(user);
        return (Integer)redis.open(new Redis.BinaryHandler() {
            @Override
            public Object handle(BinaryJedisCommands cmd) {
                Long n = cmd.llen(key);
                return n != null ? n.intValue() : 0;
            }
        });
    }

    @Override
    public TimelineEntry lastIdBefore(final Context ctx, final PeopleId user, final long max) {
        final byte[] key = makeKey(user);
        return (TimelineEntry)redis.open(new Redis.BinaryHandler() {
            @Override
            public Object handle(BinaryJedisCommands cmd) {
                if (max == Long.MAX_VALUE || max <= 0) {
                    byte[] bytes = cmd.lindex(key, 0);
                    if (bytes == null)
                        return null;

                    return TimelineEntry.fromBytes(bytes);
                } else {
                    final int FETCH_COUNT = 100;
                    for (int begin = 0; begin < Integer.MAX_VALUE - FETCH_COUNT; begin += FETCH_COUNT) {
                        List<byte[]> bytesList = cmd.lrange(key, begin, begin + FETCH_COUNT);
                        if (CollectionUtils.isEmpty(bytesList))
                            break;

                        for (byte[] bytes : bytesList) {
                            TimelineEntry entry = TimelineEntry.fromBytes(bytes);
                            if (entry.postId <= max)
                                return entry;
                        }

                        if (bytesList.size() < FETCH_COUNT)
                            break;
                    }
                    return null;
                }
            }
        });
    }

    @Override
    public void removeTimeline(Context ctx, PeopleId user) {
        final byte[] key = makeKey(user);
        redis.open(new Redis.BinaryHandler() {
            @Override
            public Object handle(BinaryJedisCommands cmd) {
                Redis.delete(cmd, key);
                return null;
            }
        });
    }

    @Override
    public void add(Context ctx, PeopleId user, final TimelineEntry entry) {
        final byte[] key = makeKey(user);
        redis.open(new Redis.BinaryHandler() {
            @Override
            public Object handle(BinaryJedisCommands cmd) {
                cmd.lpush(key, entry.toBytes());
                return null;
            }
        });
    }

    @Override
    public TimelineResult get(final Context ctx, final PeopleId user, final PostFilter filter0, final Page page0) {
        final PostFilter filter = regulateFilter(filter0);
        final Page page = regulatePage(page0);

        final byte[] key = makeKey(user);
        return (TimelineResult)redis.open(new Redis.BinaryHandler() {
            @Override
            public Object handle(BinaryJedisCommands cmd) {
                List<byte[]> bytesList = cmd.lrange(key, 0, MAX_ENTRY_COUNT);
                if (CollectionUtils.isEmpty(bytesList))
                    return TimelineResult.newEmpty();


                TimelineEntries timeline = TimelineEntries.fromBytes(bytesList);
                TimelineEntries te = fullFilter(null, timeline, ctx, user, filter);
                return TimelineResult.split(te, page);
            }
        });
    }

    @Override
    public TimelineEntries continuousGet(final Context ctx, final PeopleId user, final PostFilter filter0, final int limit) {
        if (limit <= 0)
            return new TimelineEntries();

        final PostFilter filter = regulateFilter(filter0);
        final int fetchCount = (filter.app == App.APP_NONE && filter.types == Post.ALL_POST_TYPES) ? limit : 100;
        final byte[] key = makeKey(user);

        return (TimelineEntries)redis.open(new Redis.BinaryHandler() {
            @Override
            public Object handle(BinaryJedisCommands cmd) {
                TimelineEntries timeline = new TimelineEntries();
                for (int begin = 0; ; begin += fetchCount) {
                    List<byte[]> bytesList = cmd.lrange(key, begin, begin + fetchCount);
                    if (CollectionUtils.isEmpty(bytesList))
                        break;

                    TimelineEntries subTimeline = TimelineEntries.fromBytes(bytesList);
                    TimelineEntry firstEntry = subTimeline.get(0);
                    if (firstEntry.postId < filter.min)
                        break;

                    fullFilter(timeline, subTimeline, ctx, user, filter);
                    if (timeline.size() > limit) {
                        CollectionsHelper.trimSize(timeline, limit);
                        break;
                    }
                }

                return timeline;
            }
        });
    }
}
