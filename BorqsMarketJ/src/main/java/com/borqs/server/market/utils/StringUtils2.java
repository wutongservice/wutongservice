package com.borqs.server.market.utils;


import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;

public class StringUtils2 {

    public static String[] trimArray(String[] ss) {
        if (ss == null)
            return new String[0];

        ArrayList<String> l = new ArrayList<String>(ss.length);
        for (String s : ss) {
            String s1 = StringUtils.trimToNull(s);
            if (s1 != null)
                l.add(s1);
        }
        return l.toArray(new String[l.size()]);
    }

    public static String[] splitArray(String s, char sep, boolean trim) {
        String[] ss = StringUtils.split(s, sep);
        return trim ? trimArray(ss) : ss;
    }

    public static String[] splitArray(String s, String sep, boolean trim) {
        String[] ss = StringUtils.split(s, sep);
        return trim ? trimArray(ss) : ss;
    }

    public static int[] splitIntArray(String s, String sep) {
        String[] ss = splitArray(s, sep, true);
        int[] aa = new int[ss.length];
        for (int i = 0; i < aa.length; i++) {
            aa[i] = Integer.parseInt(ss[i]);
        }
        return aa;
    }

    public static long[] splitLongArray(String s, String sep) {
        String[] ss = splitArray(s, sep, true);
        long[] aa = new long[ss.length];
        for (int i = 0; i < aa.length; i++) {
            aa[i] = Long.parseLong(ss[i]);
        }
        return aa;
    }
}
