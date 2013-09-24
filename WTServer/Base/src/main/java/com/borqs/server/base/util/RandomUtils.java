package com.borqs.server.base.util;


import java.util.Random;

public class RandomUtils {
    private static final Random RANDOM = new Random();

    public static String generateRandomNumberString(int len) {
        Random random = new Random();
        StringBuilder buff = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            buff.append(random.nextInt(10));
        }
        return buff.toString();
    }

    public static synchronized long generateId() {
        return generateId(DateUtils.nowMillis());
    }

    public static synchronized long generateId(long timestamp) {
        return (timestamp << 21) | ((long)RANDOM.nextInt(2097152));
    }
}
