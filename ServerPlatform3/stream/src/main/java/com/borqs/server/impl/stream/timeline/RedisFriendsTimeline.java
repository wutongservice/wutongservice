package com.borqs.server.impl.stream.timeline;


import com.borqs.server.platform.cache.redis.Redis;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.stream.PostFilter;
import com.borqs.server.platform.feature.stream.timeline.FriendsTimeline;
import com.borqs.server.platform.feature.stream.timeline.TimelineEntries;
import com.borqs.server.platform.feature.stream.timeline.TimelineEntry;
import com.borqs.server.platform.feature.stream.timeline.TimelineResult;
import com.borqs.server.platform.io.Charsets;
import org.apache.commons.lang.ArrayUtils;
import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.BinaryJedisCommands;
import redis.clients.jedis.Transaction;

import java.util.List;

public class RedisFriendsTimeline extends FriendsTimeline {

    private Redis redis;
    private volatile int expireSeconds = 60 * 20;


    public RedisFriendsTimeline() {
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    private static byte[] makeKey(PeopleId user) {
        return Charsets.toBytes("f-" + user.toStringId());
    }

    public int getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(int expireSeconds) {
        this.expireSeconds = expireSeconds;
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
    public void add(final Context ctx, final PeopleId user, final TimelineEntry entry) {
        final byte[] key = makeKey(user);
        redis.open(new Redis.BinaryHandler() {
            @Override
            public Object handle(BinaryJedisCommands cmd) {
                BinaryJedis jedis = Redis.shard(cmd, key);
                byte[] filterBytes = jedis.lindex(key, -1);
                if (ArrayUtils.isNotEmpty(filterBytes)) {
                    PostFilter filter = PostFilter.fromBytes(filterBytes);
                    if (filterEntry(entry, ctx, user, FILTER_COLUMNS, filter))
                        jedis.lpush(key, entry.toBytes());
                }
                return null;
            }
        });
    }

    @Override
    public TimelineResult get(final Context ctx, final PeopleId user, final PostFilter filter, final Page page) {
        if (!user.isUser())
            return TimelineResult.newEmpty();

        TimelineEntries timeline = aggregateCount(ctx, user, filter, (int)page.getEnd());
        return TimelineResult.split(timeline, page);

        // TODO: with redis cache
        /*
        final byte[] key = makeKey(user);
        return (TimelineResult) redis.open(new Redis.BinaryHandler() {
            @Override
            public Object handle(BinaryJedisCommands cmd) {
                BinaryJedis jedis = Redis.shard(cmd, key);

                byte[] filterBytes = jedis.lindex(key, -1);
                boolean needAggr = true;
                if (ArrayUtils.isNotEmpty(filterBytes)) {
                    PostFilter oldFilter = PostFilter.fromBytes(filterBytes);
                    if (filter.equalsWithoutTime(oldFilter))
                        needAggr = false;
                }

                if (!needAggr) {
                    TimelineResult tr = loadTimeline(jedis, key, page);
                    jedis.expire(key, expireSeconds);
                    return tr;
                } else {
                    TimelineEntries timeline = aggregate(ctx, user, filter);
                    saveTimeline(jedis, key, timeline, filter);
                    return TimelineResult.split(timeline, page);
                }
            }
        });
        */
    }


    private static void saveTimeline(BinaryJedis jedis, byte[] key, List<TimelineEntry> timeline, PostFilter filter) {
        Transaction t = jedis.multi();
        try {
            t.del(key);
            for (TimelineEntry entry : timeline)
                t.rpush(key, entry.toBytes());
            t.rpush(key, filter.toBytes());
        } finally {
            t.exec();
        }
    }

    private static TimelineResult loadTimeline(BinaryJedis jedis, byte[] key, Page page) {
        if (!jedis.exists(key))
            return null;

        int len = jedis.llen(key).intValue() - 1;
        if (len < 0)
            len = 0;
        int start = (int) page.getBegin();
        int end = (int) page.getEnd();
        if (end > len - 1)
            end = len - 1;
        List<byte[]> bytesList = jedis.lrange(key, start, end);
        TimelineEntries te = TimelineEntries.fromBytes(bytesList);
        return new TimelineResult(te, len);
    }
}
