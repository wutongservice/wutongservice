package com.borqs.server.impl.stream.timeline;


import com.borqs.server.platform.cache.redis.Redis;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.stream.PostFilter;
import com.borqs.server.platform.feature.stream.timeline.TimelineEntries;
import com.borqs.server.platform.feature.stream.timeline.TimelineEntry;
import com.borqs.server.platform.feature.stream.timeline.TimelineResult;
import com.borqs.server.platform.feature.stream.timeline.WallTimeline;
import com.borqs.server.platform.io.Charsets;
import org.apache.commons.collections.CollectionUtils;
import redis.clients.jedis.BinaryJedisCommands;

import java.util.List;

public class RedisWallTimeline extends WallTimeline {

    private Redis redis;

    private volatile int storageLimit = 100000;

    public RedisWallTimeline() {
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public int getStorageLimit() {
        return storageLimit;
    }

    public void setStorageLimit(int storageLimit) {
        this.storageLimit = storageLimit;
    }

    private static byte[] makeKey(PeopleId user) {
        return Charsets.toBytes("w-" + user.toStringId());
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
                cmd.lpush(key, entry.toBytes());
                long len = cmd.llen(key);
                if (len > storageLimit) {
                    for (int i = 0; i < len - storageLimit; i++)
                        cmd.rpop(key);
                }
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
                List<byte[]> bytesList = cmd.lrange(key, 0, storageLimit);
                if (CollectionUtils.isEmpty(bytesList))
                    return TimelineResult.newEmpty();

                TimelineEntries timeline = TimelineEntries.fromBytes(bytesList);
                TimelineEntries te = fullFilter(null, timeline, ctx, user, filter);
                return TimelineResult.split(te, page);
            }
        });
    }
}
