package com.borqs.server.market.utils;


import java.util.Random;

public class RandomUtils2 {
    private static final Random rand = new Random(DateTimeUtils.nowMillis());

    public static long randomLong() {
        return (DateTimeUtils.nowMillis() << 21) | rand.nextInt(2097152);
    }
}
