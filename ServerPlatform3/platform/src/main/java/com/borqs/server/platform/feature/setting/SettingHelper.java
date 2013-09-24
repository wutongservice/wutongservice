package com.borqs.server.platform.feature.setting;


import com.borqs.server.platform.context.Context;

public class SettingHelper {
    public static void toggle(Context ctx, SettingLogic logic, String key, boolean b) {
        logic.set(ctx, key, SettingValues.toggleValue(b));
    }

    public static boolean isToggleOn(Context ctx, SettingLogic logic, long userId, String key, boolean def) {
        String b = logic.get(ctx, userId, key, SettingValues.toggleValue(def));
        return b.equals(SettingValues.ON);
    }

    public static boolean isToggleOn(Context ctx, SettingLogic logic, String key, boolean def) {
        return isToggleOn(ctx, logic, ctx.getViewer(), key, def);
    }
}
