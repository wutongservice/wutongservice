package com.borqs.server.impl.stream.timeline;


import com.borqs.server.platform.cache.redis.Redis;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.stream.PostFilter;
import com.borqs.server.platform.feature.stream.timeline.PublicTimeline;
import com.borqs.server.platform.feature.stream.timeline.TimelineEntries;
import com.borqs.server.platform.feature.stream.timeline.TimelineEntry;
import com.borqs.server.platform.feature.stream.timeline.TimelineResult;
import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.ParamChecker;
import org.apache.commons.collections.CollectionUtils;
import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.BinaryJedisCommands;

import java.util.List;

public class RedisPublicTimeline extends PublicTimeline {
    private Redis redis;
    private volatile int limit = 1000;

    public RedisPublicTimeline() {
    }

    public RedisPublicTimeline(Redis redis) {
        this.redis = redis;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    private static final byte[] KEY = Charsets.toBytes("p-all");

    private static byte[] makeKey() {
        return KEY;
    }

    @Override
    public void removeTimeline(Context ctx, PeopleId user) {
        redis.open(new Redis.BinaryHandler() {
            @Override
            public Object handle(BinaryJedisCommands cmd) {
                Redis.delete(cmd, makeKey());
                return null;
            }
        });
    }

    @Override
    public void add(final Context ctx, final PeopleId user, final TimelineEntry entry) {
        final byte[] key = makeKey();
        redis.open(new Redis.BinaryHandler() {
            @Override
            public Object handle(BinaryJedisCommands cmd) {
                cmd.lpush(key, entry.toBytes());
                int len = cmd.llen(key).intValue();
                if (len > limit) {
                    for (int i = 0; i < len - limit; i++)
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
        ParamChecker.mustTrue("page.page", page.page == 0, "page.page must be 0");

        final byte[] key = makeKey();
        TimelineEntries te = (TimelineEntries)redis.open(new Redis.BinaryHandler() {
            @Override
            public Object handle(BinaryJedisCommands cmd) {
                BinaryJedis jedis = Redis.shard(cmd, key);
                int count = (int)page.getEnd();
                TimelineEntries te = new TimelineEntries();
                for (; ; ) {
                    List<byte[]> bytesList = jedis.lrange(key, 0, 100);
                    if (CollectionUtils.isEmpty(bytesList))
                        break;

                    TimelineEntries timeline = TimelineEntries.fromBytes(bytesList);
                    fullFilter(te, timeline, ctx, user, filter);
                    if (te.size() >= count) {
                        CollectionsHelper.trimSize(te, count);
                        break;
                    }
                }
                return te;
            }
        });
        return new TimelineResult(te, 0);
    }
}
