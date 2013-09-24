package com.borqs.server.platform.feature.login;


import com.borqs.server.platform.hook.Hook;

public interface LoginHook extends Hook<LoginHook.Info> {

    static class Info {
        public final String name;
        public final long userId;
        public final String ticket;

        public Info(String name, long userId, String ticket) {
            this.name = name;
            this.userId = userId;
            this.ticket = ticket;
        }
    }

}
