package com.borqs.server.base.util;


import com.borqs.server.base.data.Values;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.Validate;

import java.util.*;

public class CollectionUtils2 {

    public static Set<String> immutableSet(String... s) {
        HashSet<String> set = new HashSet<String>();
        Collections.addAll(set, s);
        return Collections.unmodifiableSet(set);
    }

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




    private static <K, V> void putNotNull(Map<K, V> m, K k, V v) {
        if (v != null)
            m.put(k, v);
    }
    public static <K, V> Map<K, V> ofNotNull(K k1, V v1) {
        Map<K, V> m = new LinkedHashMap<K, V>();
        putNotNull(m, k1, v1);
        return m;
    }

    public static <K, V> Map<K, V> ofNotNull(K k1, V v1, K k2, V v2) {
        Map<K, V> m = new LinkedHashMap<K, V>();
        putNotNull(m, k1, v1);
        putNotNull(m, k2, v2);
        return m;
    }

    public static <K, V> Map<K, V> ofNotNull(K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> m = new LinkedHashMap<K, V>();
        putNotNull(m, k1, v1);
        putNotNull(m, k2, v2);
        putNotNull(m, k3, v3);
        return m;
    }

    public static <K, V> Map<K, V> ofNotNull(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        Map<K, V> m = new LinkedHashMap<K, V>();
        putNotNull(m, k1, v1);
        putNotNull(m, k2, v2);
        putNotNull(m, k3, v3);
        putNotNull(m, k4, v4);
        return m;
    }

    public static <K, V> Map<K, V> ofNotNull(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        Map<K, V> m = new LinkedHashMap<K, V>();
        putNotNull(m, k1, v1);
        putNotNull(m, k2, v2);
        putNotNull(m, k3, v3);
        putNotNull(m, k4, v4);
        putNotNull(m, k5, v5);
        return m;
    }


    public static Map<String, Object> arraysToMap(Object[][] arrays) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        for (Object[] e : arrays) {
            data.put(e[0].toString(), e[1]);
        }
        return data;
    }

    public static <E> Set<E> asSet(E... a) {
        LinkedHashSet<E> set = new LinkedHashSet<E>();
        for (E e : a)
            set.add(e);
        return set;
    }

    public static <E> Set<E> asSet(Collection<E> c) {
        Validate.notNull(c);
        return c instanceof Set ? (Set<E>)c : new LinkedHashSet<E>(c);
    }

    public static <E> boolean containsOne(Collection<E> c, Collection<E> c2) {
        for (E e2 : c2) {
            if (c.contains(e2))
                return true;
        }
        return false;
    }

    public static <E> boolean containsOne(Collection<E> c, E... a) {
        return containsOne(c, Arrays.asList(a));
    }

    public static <E> boolean containsAll(Collection<E> c, Collection<E> c2) {
        return c.containsAll(c2);
    }

    public static <E> boolean containsAll(Collection<E> c, E... a) {
        return containsAll(c, Arrays.asList(a));
    }

    public static <K, V> V getMapFirstValue(Map<K, V> map, V def) {
        if (map.isEmpty())
            return def;

        Iterator<V> iter = map.values().iterator();
        return iter.hasNext() ? iter.next() : def;
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
    public static long[] toLongArray(Collection c) {
        if (CollectionUtils.isEmpty(c))
            return new long[0];

        long[] r = new long[c.size()];
        int i = 0;
        for (Object o : c)
            r[i++] = Values.toInt(o);
        return r;
    }
}
