package com.borqs.server.platform.cache;


import java.util.ArrayList;
import java.util.Collection;

public abstract class AbstractCache implements Cache {
    protected AbstractCache() {
    }

    @Override
    public void put(String key, Object value, long expirySeconds) {
        put(CacheElement.forSet(key, value, expirySeconds));
    }

    @Override
    public void put(String key, Object value) {
        put(CacheElement.forSet(key, value));
    }

    @Override
    public Object getValue(String key) {
        CacheElement elem = get(key);
        return elem != null ? elem.getValue() : null;
    }

    @Override
    public void puts(Collection<CacheElement> values) {
        for (CacheElement value : values)
            put(value);
    }

    @Override
    public Collection<CacheElement> gets(Collection<String> keys) {
        ArrayList<CacheElement> values = new ArrayList<CacheElement>();
        for (String key : keys)
            values.add(get(key));
        return values;
    }

    @Override
    public void deletes(Collection<String> keys) {
        for (String key : keys)
            delete(key);
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
