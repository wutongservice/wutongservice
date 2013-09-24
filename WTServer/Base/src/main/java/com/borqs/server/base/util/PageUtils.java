package com.borqs.server.base.util;


import org.apache.commons.lang.ArrayUtils;

public class PageUtils {
    public static long[] page(long[] a, int page, int count) {
        if (page < 0)
            page = 0;
        if (count <= 0)
            count = 20;

        int begin = page * count;
        int end = begin + count;

        if (begin >= a.length)
            return new long[0];

        return ArrayUtils.subarray(a, begin, end);
    }
}
