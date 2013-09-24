package com.borqs.server.platform.cache;


import com.borqs.server.platform.util.Copyable;

public class CacheElement implements Copyable<CacheElement> {
    private String key;
    private Object value;
    private long expirySeconds = 0;

    private CacheElement() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean hasExpiry() {
        return expirySeconds > 0;
    }

    public long getExpirySeconds() {
        return expirySeconds;
    }

    public void setExpirySeconds(long expirySeconds) {
        this.expirySeconds = expirySeconds >= 0 ? expirySeconds : 0;
    }

    @Override
    public CacheElement copy() {
        CacheElement cache = new CacheElement();
        cache.key = key;
        cache.value = value;
        cache.expirySeconds = expirySeconds;
        return cache;
    }

    public static CacheElement ofKey(String key, long expirySeconds) {
        CacheElement elem = new CacheElement();
        elem.setKey(key);
        elem.setExpirySeconds(expirySeconds);
        return elem;
    }

    public static CacheElement forSet(String key, Object value, long expirySeconds) {
        CacheElement elems = new CacheElement();
        elems.setKey(key);
        elems.setValue(value);
        elems.setExpirySeconds(expirySeconds);
        return elems;
    }

    public static CacheElement forSet(String key, Object value) {
        return forSet(key, value, 0);
    }

    public static CacheElement forResult(String key, Object value) {
        return forSet(key, value, 0);
    }

    public CacheElement forSet(Object value) {
        CacheElement elem = new CacheElement();
        elem.setKey(key);
        elem.setExpirySeconds(expirySeconds);
        elem.setValue(value);
        return elem;
    }


}
