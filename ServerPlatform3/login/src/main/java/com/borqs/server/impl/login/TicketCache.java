package com.borqs.server.impl.login;


import com.borqs.server.platform.cache.Cache;
import com.borqs.server.platform.cache.FlagCache;

public class TicketCache extends FlagCache<Cache> {
    public TicketCache() {
    }

    public static String makeKey(String ticket) {
        return "tk." + ticket;
    }

    public void setUserId(String ticket, long userId) {
        // TODO: xx
    }

    public long getUserId(String ticket) {
        // TODO: xx
        return 0L;
    }

    public void deleteUserId(String ticket) {
        // TODO: xx
    }
}
