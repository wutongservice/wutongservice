package com.borqs.server.platform.util;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;

public class ThreadHelper {
    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new ServerException(E.SLEEP, e);
        }
    }

    public static void sleepSilent(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
