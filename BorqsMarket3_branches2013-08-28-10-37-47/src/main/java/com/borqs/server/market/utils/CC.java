package com.borqs.server.market.utils;


import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.util.LinkedHashMap;
import java.util.Map;

public class CC {

    public static Map<String, Object> map(Map<String, Object> m, Object... kvs) {
        if (kvs != null && kvs.length % 2 != 0)
            throw new IllegalArgumentException("kvs length error");

        if (m == null)
            m = new LinkedHashMap<String, Object>();

        if (kvs != null) {
            for (int i = 0; i < kvs.length; i += 2) {
                String key = StringUtils.trim(
                        StringUtils.removeEnd(
                                StringUtils.trim(
                                        ObjectUtils.toString(kvs[i])), "=>"));
                Object val = kvs[i + 1];
                m.put(key, val);
            }
        }

        return m;
    }

    public static Map<String, Object> map(Object... kvs) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
        return map(m, kvs);
    }

    public static Map<String, String> strMap(Map<String, String> m, String... kvs) {
        if (kvs != null && kvs.length % 2 != 0)
            throw new IllegalArgumentException("kvs length error");

        if (m == null)
            m = new LinkedHashMap<String, String>();

        if (kvs != null) {
            for (int i = 0; i < kvs.length; i += 2) {
                String key = StringUtils.trim(
                        StringUtils.removeEnd(
                                StringUtils.trim(kvs[i]), "=>"));
                String val = kvs[i + 1];
                m.put(key, val);
            }
        }

        return m;
    }

    public static Map<String, String> strMap(String... kvs) {
        LinkedHashMap<String, String> m = new LinkedHashMap<String, String>();
        return strMap(m, kvs);
    }
}
