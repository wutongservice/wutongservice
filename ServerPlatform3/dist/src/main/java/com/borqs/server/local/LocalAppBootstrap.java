package com.borqs.server.local;


import com.borqs.server.platform.app.AppBootstrap;
import com.borqs.server.platform.util.SystemHelper;

public class LocalAppBootstrap {
    static void checkHome() {
        String home = SystemHelper.getHomeDirectory();
        if (home == null)
            throw new RuntimeException("Missing env BS_HOME");
    }

    public static void main(String[] args) throws Throwable {
        checkHome();
        System.setProperty(SystemHelper.LOCAL_PROPERTY_KEY, "true");
        AppBootstrap.main(args);
    }
}
