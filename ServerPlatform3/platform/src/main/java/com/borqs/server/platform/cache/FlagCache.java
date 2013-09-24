package com.borqs.server.platform.cache;


public class FlagCache<T extends Cache> {
    public T cache;
    public boolean flag = true;

    public FlagCache() {
    }

    public boolean enabled() {
        return CacheHelper.cacheEnabled(cache, flag);
    }
}
