package com.borqs.server.platform.cache;


import org.apache.commons.lang.ObjectUtils;

public class CacheKeys {
    public static final String USER_PREFIX = "u/";
    public static final String USER_SUGGESTION_PREFIX = "u/s/";

    public static String make(String prefix, String id) {
        return ObjectUtils.toString(prefix, "") + ObjectUtils.toString(id);
    }

    public static String make(String prefix, long id) {
        return ObjectUtils.toString(prefix) + Long.toString(id);
    }

    public static CacheElement makeElement(String prefix, String id, long seconds) {
        return CacheElement.ofKey(make(prefix, id), seconds);
    }

    public static CacheElement makeElement(String prefix, long id, long seconds) {
        return CacheElement.ofKey(make(prefix, id), seconds);
    }
}
