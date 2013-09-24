package com.borqs.server.wutong.account2.util;


import com.borqs.server.base.data.Values;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.Validate;

import java.util.*;

public class CollectionsHelper {

    public static <K, V> Map<K, V> of() {
        return new LinkedHashMap<K, V>();
    }

    public static <K, V> Map<K, V> of(K k1, V v1) {
        Map<K, V> m = new LinkedHashMap<K, V>();
        m.put(k1, v1);
        return m;
    }

    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
        Map<K, V> m = new LinkedHashMap<K, V>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> m = new LinkedHashMap<K, V>();
        m.put(k1, v1);
        m.put(k2, v2);
        m.put(k3, v3);
        return m;
    }

    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        Map<K, V> m = new LinkedHashMap<K, V>();
        m.put(k1, v1);
        m.put(k2, v2);
        m.put(k3, v3);
        m.put(k4, v4);
        return m;
    }

    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        Map<K, V> m = new LinkedHashMap<K, V>();
        m.put(k1, v1);
        m.put(k2, v2);
        m.put(k3, v3);
        m.put(k4, v4);
        m.put(k5, v5);
        return m;
    }

//    private static <K, V> void putNotNull(Map<K, V> m, K k, V v) {
//        if (v != null)
//            m.put(k, v);
//    }
//    public static <K, V> Map<K, V> ofNotNull(K k1, V v1) {
//        Map<K, V> m = new LinkedHashMap<K, V>();
//        putNotNull(m, k1, v1);
//        return m;
//    }
//
//    public static <K, V> Map<K, V> ofNotNull(K k1, V v1, K k2, V v2) {
//        Map<K, V> m = new LinkedHashMap<K, V>();
//        putNotNull(m, k1, v1);
//        putNotNull(m, k2, v2);
//        return m;
//    }
//
//    public static <K, V> Map<K, V> ofNotNull(K k1, V v1, K k2, V v2, K k3, V v3) {
//        Map<K, V> m = new LinkedHashMap<K, V>();
//        putNotNull(m, k1, v1);
//        putNotNull(m, k2, v2);
//        putNotNull(m, k3, v3);
//        return m;
//    }
//
//    public static <K, V> Map<K, V> ofNotNull(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
//        Map<K, V> m = new LinkedHashMap<K, V>();
//        putNotNull(m, k1, v1);
//        putNotNull(m, k2, v2);
//        putNotNull(m, k3, v3);
//        putNotNull(m, k4, v4);
//        return m;
//    }
//
//    public static <K, V> Map<K, V> ofNotNull(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
//        Map<K, V> m = new LinkedHashMap<K, V>();
//        putNotNull(m, k1, v1);
//        putNotNull(m, k2, v2);
//        putNotNull(m, k3, v3);
//        putNotNull(m, k4, v4);
//        putNotNull(m, k5, v5);
//        return m;
//    }


    public static Map<String, Object> arraysToMap(Object[][] arrays) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        for (Object[] e : arrays)
            data.put(e[0].toString(), e[1]);

        return data;
    }

    public static <E> Set<E> asSet(E... a) {
        LinkedHashSet<E> set = new LinkedHashSet<E>();
        Collections.addAll(set, a);
        return set;
    }

    public static <E> Set<E> asSet(Collection<E> c) {
        Validate.notNull(c);
        return c instanceof Set ? (Set<E>)c : new LinkedHashSet<E>(c);
    }

    public static Set<Integer> asSet(int... a) {
        LinkedHashSet<Integer> set = new LinkedHashSet<Integer>();
        for (int v : a)
            set.add(v);
        return set;
    }

    public static Set<Long> asSet(long... a) {
        LinkedHashSet<Long> set = new LinkedHashSet<Long>();
        for (long v : a)
            set.add(v);
        return set;
    }

    public static <E> boolean containsAny(Collection<E> c, Collection<E> c2) {
        for (E e2 : c2) {
            if (c.contains(e2))
                return true;
        }
        return false;
    }

    public static <E> boolean containsAny(Collection<E> c, E... a) {
        return containsAny(c, Arrays.asList(a));
    }

    public static <E> boolean containsAll(Collection<E> c, Collection<E> c2) {
        return c.containsAll(c2);
    }

    public static <E> boolean containsAll(Collection<E> c, E... a) {
        return containsAll(c, Arrays.asList(a));
    }

    public static boolean joint(Collection<?> c1, Collection<?> c2) {
        return !Collections.disjoint(c1, c2);
    }

    public static <T> Set<T> subtract(Collection<? extends T> sup, Collection<? extends T> sub) {
        LinkedHashSet<T> s = new LinkedHashSet<T>(sup);
        s.removeAll(sub);
        return s;
    }

    public static <T> Set<T> subtract(Collection<? extends T> sup, T... sub) {
        return subtract(sup, Arrays.asList(sub));
    }

    public static long[] toLongArray(Collection c) {
        if (CollectionUtils.isEmpty(c))
            return new long[0];

        long[] r = new long[c.size()];
        int i = 0;
        for (Object o : c)
            r[i++] = Values.toInt(o);
        return r;
    }

    public static int[] toIntArray(Collection c) {
        if (CollectionUtils.isEmpty(c))
            return new int[0];

        int[] r = new int[c.size()];
        int i = 0;
        for (Object o : c)
            r[i++] = (int) Values.toInt(o);
        return r;
    }

    public static <T> ArrayList<T> ofArrayList(T... arr) {
        ArrayList<T> l = new ArrayList<T>();
        Collections.addAll(l, arr);
        return l;
    }

    public static <K, V> V getMapFirstValue(Map<K, V> map, V def) {
        if (map.isEmpty())
            return def;

        Iterator<V> iter = map.values().iterator();
        return iter.hasNext() ? iter.next() : def;
    }

    public static <T> boolean setEquals(Set<T> set1, Set<T> set2) {
        return new TreeSet<T>(set1).equals(new TreeSet<T>(set2));
    }

    public static String[] removeElements(String[] a, String[] remove) {
        ArrayList<String> l = new ArrayList<String>();
        for (String elem : a) {
            if (!ArrayUtils.contains(remove, elem))
                l.add(elem);
        }
        return l.toArray(new String[l.size()]);
    }
}
