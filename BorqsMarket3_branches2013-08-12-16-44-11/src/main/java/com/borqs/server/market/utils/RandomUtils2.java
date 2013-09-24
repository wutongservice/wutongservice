package com.borqs.server.market.utils;


import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

public class RandomUtils2 {
    private static final Random rand = new Random(DateTimeUtils.nowMillis());

    public static long randomLongWith(long ts) {
        return (ts << 21) | rand.nextInt(2097152);
    }

    public static long randomLong() {
        return randomLongWith(DateTimeUtils.nowMillis());
    }

    public static String randomString(int bits) {
        return new BigInteger(bits, rand).toString(32);
    }

    public static int randomInt(int min, int max) {
        return min + rand.nextInt(max - min);
    }
}
