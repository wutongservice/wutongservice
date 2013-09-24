package com.borqs.server.platform.feature.setting;


import com.borqs.server.platform.hook.Hook;

import java.util.Map;

public interface SetSettingHook extends Hook<SetSettingHook.Info> {

    public static class Info {
        public final long userId;
        public final Map<String, String> setting;

        public Info(long userId, Map<String, String> setting) {
            this.userId = userId;
            this.setting = setting;
        }
    }

}
