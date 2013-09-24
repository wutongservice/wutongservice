package com.borqs.server.platform.cache.redis;


import com.borqs.server.platform.cache.AbstractCache;
import com.borqs.server.platform.cache.CacheElement;
import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.io.IOHelper;
import org.apache.commons.lang.Validate;
import redis.clients.jedis.*;
import redis.clients.util.Sharded;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class Redis extends AbstractCache {
    protected final JedisPoolConfig config = new JedisPoolConfig();

    protected Redis() {
    }

    public int getMaxIdle() {
        return config.getMaxIdle();
    }

    public void setMaxIdle(int maxIdle) {
        config.setMaxIdle(maxIdle);
    }

    public int getMinIdle() {
        return config.getMinIdle();
    }

    public void setMinIdle(int minIdle) {
        config.setMinIdle(minIdle);
    }

    public int getMaxActive() {
        return config.getMaxActive();
    }

    public void setMaxActive(int maxActive) {
        config.setMaxActive(maxActive);
    }

    public long getMaxWait() {
        return config.getMaxWait();
    }

    public void setMaxWait(long maxWait) {
        config.setMaxWait(maxWait);
    }

    public byte getWhenExhaustedAction() {
        return config.getWhenExhaustedAction();
    }

    public void setWhenExhaustedAction(byte whenExhaustedAction) {
        config.setWhenExhaustedAction(whenExhaustedAction);
    }

    public boolean isTestOnBorrow() {
        return config.isTestOnBorrow();
    }

    public void setTestOnBorrow(boolean testOnBorrow) {
        config.setTestOnBorrow(testOnBorrow);
    }

    public boolean isTestOnReturn() {
        return config.isTestOnReturn();
    }

    public void setTestOnReturn(boolean testOnReturn) {
        config.setTestOnReturn(testOnReturn);
    }

    public boolean isTestWhileIdle() {
        return config.isTestWhileIdle();
    }

    public void setTestWhileIdle(boolean testWhileIdle) {
        config.setTestWhileIdle(testWhileIdle);
    }

    public long getTimeBetweenEvictionRunsMillis() {
        return config.getTimeBetweenEvictionRunsMillis();
    }

    public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        config.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
    }

    public int getNumTestsPerEvictionRun() {
        return config.getNumTestsPerEvictionRun();
    }

    public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        config.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
    }

    public long getMinEvictableIdleTimeMillis() {
        return config.getMinEvictableIdleTimeMillis();
    }

    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        config.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
    }

    public long getSoftMinEvictableIdleTimeMillis() {
        return config.getSoftMinEvictableIdleTimeMillis();
    }

    public void setSoftMinEvictableIdleTimeMillis(long softMinEvictableIdleTimeMillis) {
        config.setSoftMinEvictableIdleTimeMillis(softMinEvictableIdleTimeMillis);
    }

    public abstract Object open(Handler handler);

    public abstract Object open(BinaryHandler handler);

    @Override
    public void put(final CacheElement value) {
        Validate.notNull(value);
        open(new BinaryHandler() {
            @Override
            public Object handle(BinaryJedisCommands cmd) {
                byte[] keyBytes = Charsets.toBytes(value.getKey());
                byte[] valueBytes = IOHelper.toBytes(value.getValue());
                if (value.getExpirySeconds() == 0) {
                    cmd.set(keyBytes, valueBytes);
                } else {
                    cmd.setex(keyBytes, (int) value.getExpirySeconds(), valueBytes);
                }
                return null;
            }
        });

    }

    @Override
    public void puts(Collection<CacheElement> values) {
        // TODO: invoke mset command
        super.puts(values);
    }

    @Override
    public CacheElement get(final String key) {
        return (CacheElement) open(new BinaryHandler() {
            @Override
            public Object handle(BinaryJedisCommands cmd) {
                byte[] valueBytes = cmd.get(Charsets.toBytes(key));
                if (valueBytes != null) {
                    return CacheElement.forResult(key, IOHelper.fromBytes(valueBytes));
                } else {
                    return CacheElement.forResult(key, null);
                }
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<CacheElement> gets(final Collection<String> keys) {
        Validate.notNull(keys);

        return (Collection)open(new BinaryHandler() {
            @Override
            public Object handle(BinaryJedisCommands cmd) {
                if (cmd instanceof Sharded)
                    return Redis.super.gets(keys);

                ArrayList<CacheElement> r = new ArrayList<CacheElement>();
                if (keys.isEmpty())
                    return r;

                if (cmd instanceof BinaryJedis) {
                    byte[][] keysBytes = getKeysBytes(keys);
                    List<byte[]> valuesBytes = ((BinaryJedis) cmd).mget(keysBytes);
                    for (int i = 0; i < keys.size(); i++) {
                        byte[] valueBytes = valuesBytes.get(i);
                        r.add(CacheElement.forResult(Charsets.fromBytes(keysBytes[i]), valueBytes != null ? IOHelper.fromBytes(valueBytes) : null));
                    }
                }

                return r;
            }
        });
    }

    private static byte[][] getKeysBytes(Collection<String> keys) {
        ArrayList<byte[]> l = new ArrayList<byte[]>();
        for (String key : keys)
            l.add(Charsets.toBytes(key));
        return l.toArray(new byte[l.size()][]);
    }

    @Override
    public void delete(final String key) {
        open(new BinaryHandler() {
            @Override
            public Object handle(BinaryJedisCommands cmd) {
                if (cmd instanceof ShardedJedis) {
                    ((ShardedJedis) cmd).del(key);
                } else if (cmd instanceof Jedis) {
                    ((Jedis) cmd).del(key);
                }
                return null;
            }
        });
    }

    public void publish(final String channel, final String value) {
        open(new BinaryHandler() {
            @Override
            public Object handle(BinaryJedisCommands cmd) {
                byte[] channelBytes = Charsets.toBytes(channel);
                BinaryJedis jedis = shard(cmd, channelBytes);
                jedis.publish(channelBytes, Charsets.toBytes(value));
                return null;
            }
        });
    }


    public static BinaryJedis shard(BinaryJedisCommands cmd, byte[] key) {
        if (cmd instanceof BinaryShardedJedis) {
            return ((BinaryShardedJedis) cmd).getShard(key);
        } else if (cmd instanceof BinaryJedis) {
            return (BinaryJedis) cmd;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static void delete(BinaryJedisCommands cmd, byte[] key) {
        BinaryJedis jedis = shard(cmd, key);
        jedis.del(key);
    }

    public static interface Handler {
        Object handle(JedisCommands cmd);
    }

    public static interface BinaryHandler {
        Object handle(BinaryJedisCommands cmd);
    }
}
