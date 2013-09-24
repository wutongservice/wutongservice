package com.borqs.server.platform.cache;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.Validate;

import java.util.*;
import java.util.regex.Pattern;

public class MultipleCache implements Cache {
    private Cache defaultCache;
    private final List<TheCache> caches = new ArrayList<TheCache>();

    public MultipleCache() {
    }

    public Cache getDefaultCache() {
        return defaultCache;
    }

    public void setDefaultCache(Cache defaultCache) {
        this.defaultCache = defaultCache;
    }

    public Map<String, Cache> getCaches() {
        HashMap<String, Cache> m = new HashMap<String, Cache>();
        for (TheCache c : caches)
            m.put(c.keyPattern.pattern(), c.cache);
        return m;
    }

    public void setCaches(Map<String, Cache> caches) {
        caches.clear();
        if (MapUtils.isNotEmpty(caches)) {
            for (Map.Entry<String, Cache> e : caches.entrySet())
                this.caches.add(new TheCache(e.getKey(), e.getValue()));
        }
    }

    private Cache dispatch(String key) {
        Validate.notNull(key);
        for (TheCache c : caches) {
            if (c.match(key))
                return c.cache;
        }
        if (defaultCache != null)
            return defaultCache;

        throw new ServerException(E.CACHE, "Display cache error '%s'", key);
    }

    private Map<Cache, List<CacheElement>> groupElementsDispatch(Collection<CacheElement> elems) {
        HashMap<Cache, List<CacheElement>> m = new HashMap<Cache, List<CacheElement>>();
        for (CacheElement elem : elems) {
            Cache c = dispatch(elem.getKey());
            List<CacheElement> l = m.get(c);
            if (l == null) {
                l = new ArrayList<CacheElement>();
                m.put(c, l);
            }
            l.add(elem);
        }
        return m;
    }

    private Map<Cache, List<String>> groupKeysDispatch(Collection<String> keys) {
        HashMap<Cache, List<String>> m = new HashMap<Cache, List<String>>();
        for (String key : keys) {
            Cache c = dispatch(key);
            List<String> l = m.get(c);
            if (l == null) {
                l = new ArrayList<String>();
                m.put(c, l);
            }
            l.add(key);
        }
        return m;
    }

    @Override
    public void put(CacheElement value) {
        dispatch(value.getKey()).put(value);
    }

    @Override
    public void put(String key, Object value, long expirySeconds) {
        dispatch(key).put(key, value, expirySeconds);
    }

    @Override
    public void put(String key, Object value) {
        dispatch(key).put(key, value);
    }

    @Override
    public CacheElement get(String key) {
        return dispatch(key).get(key);
    }

    @Override
    public Object getValue(String key) {
        return dispatch(key).getValue(key);
    }

    @Override
    public void delete(String key) {
        dispatch(key).delete(key);
    }

    @Override
    public void puts(Collection<CacheElement> values) {
        Map<Cache, List<CacheElement>> grouped = groupElementsDispatch(values);
        for (Map.Entry<Cache, List<CacheElement>> e : grouped.entrySet()) {
            e.getKey().puts(e.getValue());
        }
    }

    @Override
    public Collection<CacheElement> gets(Collection<String> keys) {
        ArrayList<CacheElement> r = new ArrayList<CacheElement>();
        Map<Cache, List<String>> grouped = groupKeysDispatch(keys);
        for (Map.Entry<Cache, List<String>> e : grouped.entrySet()) {
            r.addAll(e.getKey().gets(e.getValue()));
        }
        return r;
    }

    @Override
    public void deletes(Collection<String> keys) {
        Map<Cache, List<String>> grouped = groupKeysDispatch(keys);
        for (Map.Entry<Cache, List<String>> e : grouped.entrySet()) {
            e.getKey().deletes(e.getValue());
        }
    }


    private static class TheCache {
        final Pattern keyPattern;
        final Cache cache;

        private TheCache(String keyPatt, Cache cache) {
            this.keyPattern = Pattern.compile(keyPatt);
            this.cache = cache;
        }

        public boolean match(String key) {
            return keyPattern.matcher(key).matches();
        }
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
