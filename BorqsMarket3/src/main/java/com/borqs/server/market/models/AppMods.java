package com.borqs.server.market.models;


import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.mybatis.record.RecordSession;

public class AppMods {

    private static final String[][] DEFAULT_MODS = {
            {"com.borqs.freehdhome", "wallpaper", "landscape"},
    };

    public static String getDefaultAppMod(RecordSession session, String appId, String categoryId) {
        // TODO: ...
        for (String[] item : DEFAULT_MODS) {
            if (item[0].equals(appId) && item[1].equals(categoryId))
                return item[2];
        }
        return null;
    }
}
