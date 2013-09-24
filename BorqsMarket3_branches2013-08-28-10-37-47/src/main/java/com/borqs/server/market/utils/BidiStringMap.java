package com.borqs.server.market.utils;


import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.lang.ObjectUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class BidiStringMap {
    private final BidiMap map = new DualHashBidiMap();

    public BidiStringMap() {
    }

    @SuppressWarnings("unchecked")
    public BidiStringMap(Map<String, String> m) {
        map.putAll(m);
    }

    public static BidiStringMap of(Object... kvs) {
        BidiStringMap bsm = new BidiStringMap();
        for (Map.Entry<String, Object> e : CC.map(kvs).entrySet()) {
            bsm.set(e.getKey(), ObjectUtils.toString(e.getValue()));
        }
        return bsm;
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public String getValueByKey(String key) {
        return (String) map.get(key);
    }

    public String getKeyByValue(String val) {
        return (String) map.getKey(val);
    }

    public boolean hasKey(String key) {
        return map.containsKey(key);
    }

    public boolean hasValue(String val) {
        return map.containsValue(val);
    }

    public void clear() {
        map.clear();
    }

    public BidiStringMap set(String key, String val) {
        map.put(key, val);
        return this;
    }

    @SuppressWarnings("unchecked")
    public String[] getKeyArray() {
        Set<String> set = map.keySet();
        return set.toArray(new String[set.size()]);
    }

    @SuppressWarnings("unchecked")
    public String[] getValueArray() {
        Collection<String> set = map.values();
        return set.toArray(new String[set.size()]);
    }
}
