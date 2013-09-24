package com.borqs.server.platform.feature.setting;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.logic.Logic;

import java.util.Map;

public interface SettingLogic extends Logic {
    void sets(Context ctx, Map<String, String> setting);
    Map<String, String> gets(Context ctx, long userId, String[] keys, Map<String, String> def);
    void set(Context ctx, String key, String value);
    String get(Context ctx, long userId, String key, String def);
    Map<String, String> getsByStartsWith(Context ctx, long userId, String keyStartsWith, Map<String, String> def);
    void delete(Context ctx, String... keys);
}
