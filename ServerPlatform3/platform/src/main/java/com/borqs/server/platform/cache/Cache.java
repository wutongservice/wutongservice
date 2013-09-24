package com.borqs.server.platform.cache;


import java.util.Collection;

public interface Cache {

    void put(CacheElement value);

    void put(String key, Object value, long expirySeconds);

    void put(String key, Object value);

    CacheElement get(String key);

    Object getValue(String key);

    void delete(String key);

    void puts(Collection<CacheElement> values);

    Collection<CacheElement> gets(Collection<String> keys);

    void deletes(Collection<String> keys);
}
