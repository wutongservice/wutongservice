package com.borqs.server.platform.util;


import java.util.*;

public class RandomHelper {
    private static final Random RANDOM = new Random();

    public static String generateUuidString() {
        return UUID.randomUUID().toString();
    }

    public static String generateRandomNumberString(int len) {
        Random random = new Random();
        StringBuilder buff = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            buff.append(random.nextInt(10));
        }
        return buff.toString();
    }

    public static long generateId() {
        return generateId(DateHelper.nowMillis());
    }

    public static synchronized long generateId(long timestamp) {
        return (timestamp << 21) | ((long) RANDOM.nextInt(2097152));
    }

    public static long getTimestamp(long random) {
        return random >> 21;
    }

    public static long timestampToId(long timestamp) {
        return (timestamp << 21);
    }

    @SuppressWarnings("unchecked")
    public static void randomRetains(Collection c, int n) {
        if (c.size() > n) {
            ArrayList l = new ArrayList(c);
            Collections.shuffle(l);
            c.clear();
            c.addAll(l.subList(0, n));
        }
    }

    public static <T> T randomSelect(List<T> l) {
        int size = l.size();
        return l.get(size == 1 ? 0 : RANDOM.nextInt(size));
    }

    public static <T> T randomSelect(T... a) {
        int size = a.length;
        return a[size == 1 ? 0 : RANDOM.nextInt(size)];
    }
}
