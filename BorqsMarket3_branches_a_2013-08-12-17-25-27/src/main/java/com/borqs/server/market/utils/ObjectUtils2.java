package com.borqs.server.market.utils;


import org.apache.commons.lang.ObjectUtils;

public class ObjectUtils2 {
    public static int hashCodeMulti(Object... objs) {
        int hash = 1;
        if (objs != null) {
            for (Object object : objs) {
                hash = hash * 31 + ObjectUtils.hashCode(object);
            }
        }
        return hash;
    }
}
