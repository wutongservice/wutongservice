package com.borqs.server.market.models;


public class AppMods {

    private static final String[][] DEFAULT_MODS = {
            {"com.borqs.freehdhome", "wallpaper", "landscape"},
    };

    public static String getDefaultAppModForPurchase(String appId, String categoryId) {
        for (String[] item : DEFAULT_MODS) {
            if (item[0].equals(appId) && item[1].equals(categoryId))
                return item[2];
        }
        return null;
    }


}
