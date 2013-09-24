package com.borqs.server.platform.web.topaz;


public class TopazHelper {
    public static void halt() {
        halt(200, "");
    }

    public static void halt(int status) {
        halt(status, "");
    }

    public static void halt(String body) {
        halt(200, body);
    }

    public static void halt(int status, String body) {
        throw new HaltException(status, body);
    }

    public static void noActionHalt() {
        throw new QuietHaltException();
    }
}
