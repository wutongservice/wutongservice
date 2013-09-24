package com.borqs.server.platform.hook;


import com.borqs.server.platform.cache.redis.Redis;

public class AbstractRedisHook {
    protected Redis redis;

    public AbstractRedisHook() {
    }

    public AbstractRedisHook(Redis redis) {
        this.redis = redis;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public void publish(String channel, String value) {
        redis.publish(channel, value);
    }
}
