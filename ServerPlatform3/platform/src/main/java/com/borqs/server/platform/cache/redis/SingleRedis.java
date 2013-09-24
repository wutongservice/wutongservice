package com.borqs.server.platform.cache.redis;


import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.util.Initializable;
import com.borqs.server.platform.util.NetAddress;
import redis.clients.jedis.BinaryJedisCommands;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class SingleRedis extends Redis implements Initializable {
    private volatile JedisPool pool;
    private String server;

    public SingleRedis() {
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    @Override
    public void init() throws Exception {
        if (pool != null)
            throw new IllegalStateException();

        NetAddress addr = NetAddress.parse(server);
        pool = new JedisPool(config, addr.host, addr.port);
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
        Jedis jedis = null;
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
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            return handler.handle(jedis);
        } finally {
            if (jedis != null)
                pool.returnResource(jedis);
        }
    }

}
