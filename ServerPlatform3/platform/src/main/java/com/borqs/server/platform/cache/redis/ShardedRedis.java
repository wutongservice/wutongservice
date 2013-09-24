package com.borqs.server.platform.cache.redis;


import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.util.Initializable;
import redis.clients.jedis.BinaryJedisCommands;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import java.util.List;

public class ShardedRedis extends Redis implements Initializable {
    private volatile ShardedJedisPool pool;
    private List<JedisShardInfo> shards;

    public ShardedRedis() {
    }

    public List<JedisShardInfo> getShards() {
        return shards;
    }

    public void setShards(List<JedisShardInfo> shards) {
        this.shards = shards;
    }

    @Override
    public void init() throws Exception {
        if (pool != null)
            throw new IllegalStateException();

        pool = new ShardedJedisPool(config, shards);
    }

    @Override
    public void destroy() {
        if (pool != null) {
            try {
                pool.destroy();
            } finally {
                pool = null;
            }
        }
    }

    @Override
    public Object open(Handler handler) {
        ShardedJedis jedis = null;
        try {
            jedis = pool.getResource();
            return handler.handle(jedis);
        } finally {
            if (jedis != null)
                pool.returnResource(jedis);
        }
    }

    @Override
    public Object open(BinaryHandler handler) {
        ShardedJedis jedis = null;
        try {
            jedis = pool.getResource();
            return handler.handle(jedis);
        } finally {
            if (jedis != null)
                pool.returnResource(jedis);
        }
    }
}
