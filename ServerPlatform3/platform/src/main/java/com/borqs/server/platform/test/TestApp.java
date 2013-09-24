package com.borqs.server.platform.test;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.app.App;
import com.borqs.server.platform.feature.app.AppLogic;

public class TestApp implements AppLogic {

    public static final int APP1_ID = 1;
    public static final int APP2_ID = 2;

    public static final String APP1_NAME = "App1";
    public static final String APP2_NAME = "App2";

    public static final String APP1_SECRET = "secret1";
    public static final String APP2_SECRET = "secret2";

    public static final App APP1 = new App(APP1_ID, APP1_SECRET, APP1_NAME);
    public static final App APP2 = new App(APP2_ID, APP2_SECRET, APP2_NAME);


    @Override
    public boolean hasApp(Context ctx, int appId) {
        return appId == APP1_ID || appId == APP2_ID;
    }

    @Override
    public App getApp(Context ctx, int appId) {
        switch (appId) {
            case APP1_ID:
                return APP1;
            case APP2_ID:
                return APP2;
        }
        return null;
    }
}
