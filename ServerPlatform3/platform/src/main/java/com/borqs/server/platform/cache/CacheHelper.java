package com.borqs.server.platform.cache;


import com.borqs.server.platform.cache.ehcache.Ehcache;
import com.borqs.server.platform.cache.memcached.Memcached;
import com.borqs.server.platform.cache.redis.Redis;
import net.rubyeye.xmemcached.MemcachedClient;

import java.util.Collection;

public class CacheHelper {
    public static boolean allHaveValue(Collection<CacheElement> ces) {
        for (CacheElement ce : ces) {
            if (ce.getValue() == null)
                return false;
        }
        return true;
    }

    public static boolean isEhcache(Cache cache) {
        return cache instanceof Ehcache;
    }

    public static net.sf.ehcache.Cache getEhcacheImpl(Cache cache) {
        return isEhcache(cache) ? ((Ehcache) cache).getEhcache() : null;
    }

    public static boolean isMemcached(Cache cache) {
        return cache instanceof Memcached;
    }

    public static MemcachedClient getMemcachedImpl(Cache cache) {
        return isMemcached(cache) ? ((Memcached) cache).getMemcached() : null;
    }

    public static boolean isRedis(Cache cache) {
        return cache instanceof Redis;
    }

    public static boolean cacheEnabled(Cache cache, boolean enabled) {
        return cache != null && enabled;
    }
}
