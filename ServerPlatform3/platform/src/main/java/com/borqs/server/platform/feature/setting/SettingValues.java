package com.borqs.server.platform.feature.setting;


public class SettingValues {

    public static final String ON = "0";
    public static final String OFF = "1";
    public static final String CONTACTS_AUTO_ADD_FRIEND_INIT = "100";

    public static String toggleValue(boolean b) {
        return b ? ON : OFF;
    }
}
