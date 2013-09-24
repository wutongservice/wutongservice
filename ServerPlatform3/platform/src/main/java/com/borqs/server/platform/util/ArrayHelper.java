package com.borqs.server.platform.util;


import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class ArrayHelper {

    public static boolean equalsAsSet(long[] a1, long[] a2) {
        if (a1 == a2)
            return true;

        if (a1 == null || a2 == null)
            return false;

        long[] a1a = ArrayUtils.clone(a1);
        long[] a2a = ArrayUtils.clone(a2);
        Arrays.sort(a1a);
        Arrays.sort(a2a);
        return Arrays.equals(a1a, a2a);
    }

    public static boolean equalsAsSet(int[] a1, int[] a2) {
        if (a1 == a2)
            return true;

        if (a1 == null || a2 == null)
            return false;

        int[] a1a = ArrayUtils.clone(a1);
        int[] a2a = ArrayUtils.clone(a2);
        Arrays.sort(a1a);
        Arrays.sort(a2a);
        return Arrays.equals(a1a, a2a);
    }

    public static boolean equalsAsSet(String[] a1, String[] a2) {
        if (a1 == a2)
            return true;

        if (a1 == null || a2 == null)
            return false;

        return CollectionsHelper.setEquals(Arrays.asList(a1), Arrays.asList(a2));
    }

    public static int[] addAsSet(int[] a, int v) {
        if (ArrayUtils.contains(a, v))
            return ArrayUtils.clone(a);
        else
            return ArrayUtils.add(a, v);
    }

    public static long[] addAsSet(long[] a, long v) {
        if (ArrayUtils.contains(a, v))
            return ArrayUtils.clone(a);
        else
            return ArrayUtils.add(a, v);
    }

    public static String[] addAsSet(String[] a, String v) {
        if (ArrayUtils.contains(a, v))
            return (String[])ArrayUtils.clone(a);
        else
            return (String[])ArrayUtils.add(a, v);
    }

    public static long[] intersection(long[] a1, long[] a2) {
        Set<Long> s1 = CollectionsHelper.asSet(a1);
        Set<Long> s2 = CollectionsHelper.asSet(a2);
        Collection r = CollectionUtils.intersection(s1, s2);
        return CollectionsHelper.toLongArray(r);
    }

    public static boolean inArray(String s, String... arr) {
        return ArrayUtils.contains(arr, s);
    }

    public static boolean inArrayIgnoreCase(String s, String... arr) {
        for (String e : arr) {
            if (StringUtils.equalsIgnoreCase(s, e))
                return true;
        }
        return false;
    }

    public static long[] removeElement(long[] arr, long removed) {
        ArrayList<Long> l = new ArrayList<Long>();
        for (long n : arr) {
            if (n != removed)
                l.add(n);
        }
        return CollectionsHelper.toLongArray(l);
    }

    public static long[] removeElements(long[] arr, long[] removed) {
        ArrayList<Long> l = new ArrayList<Long>();
        for (long n : arr) {
            if (!ArrayUtils.contains(removed, n))
                l.add(n);
        }
        return CollectionsHelper.toLongArray(l);
    }

    public static String[] merge(String[] a1, String[] a2, String[]... an) {
        ArrayList<String> l = new ArrayList<String>();
        Collections.addAll(l, a1);
        Collections.addAll(l, a2);
        for (String[] ai : an)
            Collections.addAll(l, ai);
        return l.toArray(new String[l.size()]);
    }

    public static boolean containsIgnoreCase(String[] a, String e) {
        for (String s : a) {
            if (StringUtils.equalsIgnoreCase(s, e))
                return true;
        }
        return false;
    }

    public static long[] stringArrayToLongArray(String[] a) {
        if (a == null)
            return null;
        if (a.length == 0)
            return new long[0];

        long[] r = new long[a.length];
        for (int i = 0; i < a.length; i++)
            r[i] = Long.parseLong(a[i]);

        return r;
    }

    public static String[] longArrayToStringArray(long[] a) {
        if (a == null)
            return null;
        if (a.length == 0)
            return new String[0];

        String[] r = new String[a.length];
        for (int i = 0; i < a.length; i++)
            r[i] = Long.toString(a[i]);

        return r;
    }
}
