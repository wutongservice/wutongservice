package com.borqs.server.platform.feature.cibind;


import com.borqs.server.platform.hook.Hook;

public interface CibindHook extends Hook<CibindHook.Info> {

    public static class Info {
        public final long userId;
        public final String type;
        public final String info;

        public Info(long userId, String type, String info) {
            this.userId = userId;
            this.type = type;
            this.info = info;
        }
    }

}
