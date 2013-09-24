package com.borqs.server.impl.setting;


import com.borqs.server.platform.cache.Cache;
import com.borqs.server.platform.cache.CacheElement;
import com.borqs.server.platform.cache.FlagCache;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class SettingCache extends FlagCache<Cache> {
    public SettingCache() {
    }

    public static String makeKey(long userId, String key) {
        return "st." + userId + "." + StringUtils.trimToEmpty(key);
    }

    public static List<String> makeKeys(long userId, String[] keys) {
        ArrayList<String> cacheKeys = new ArrayList<String>();
        for (String key : keys)
            cacheKeys.add(makeKey(userId , key));
        return cacheKeys;
    }

    private static String parseKey(String cacheKey) {
        return StringUtils.substringAfter(StringUtils.removeStart(cacheKey, "st."), ".");
    }

    public Map<String, String> gets(long userId, String[] keys, Collection<String> missingKeys) {
        Collection<CacheElement> ces = cache.gets(makeKeys(userId, keys));
        LinkedHashMap<String, String> m = new LinkedHashMap<String, String>();
        for (CacheElement ce : ces) {
            String key = parseKey(ce.getKey());
            if (ce.getValue() != null) {
                m.put(key, (String)ce.getValue());
            } else {
                if (missingKeys != null)
                    missingKeys.add(key);
            }
        }
        return m;
    }

    public void sets(long userId, Map<String, String> setting) {
        ArrayList<CacheElement> ces = new ArrayList<CacheElement>();
        for (Map.Entry<String, String> e : setting.entrySet()) {
            ces.add(CacheElement.forSet(makeKey(userId, e.getKey()), StringUtils.trimToEmpty(e.getValue())));
        }
        cache.puts(ces);
    }

    public void delete(long userId, String[] keys) {
        cache.deletes(makeKeys(userId, keys));
    }
}
