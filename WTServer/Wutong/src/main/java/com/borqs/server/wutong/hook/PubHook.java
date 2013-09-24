package com.borqs.server.wutong.hook;

import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.net.HostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;


public class PubHook implements PlatformHook {

    private static final Logger L = LoggerFactory.getLogger(PubHook.class);

    private Jedis getJedis() {
        String addr = GlobalConfig.get().getString("platform.pubServer", null);
        if (addr == null)
            return null;

        HostAndPort hp = HostAndPort.parse(addr);
        try {
            return new Jedis(hp.host, hp.port);
        } catch (Throwable t) {
            L.error("create jedis error", t);
            return null;
        }
    }

    private void closeJedis(Jedis jedis) {
        try {
            if (jedis != null)
                jedis.disconnect();
        } catch (Exception e) {
            L.warn("close jedis error");
        }
    }

    @Override
    public void onUserCreated(Record user) {
        Jedis jedis = getJedis();
        if (jedis != null) {
            try {
                jedis.publish("PlatformHook.onUserCreated", user.toString());
            } catch (Exception e) {
                L.error("PubHook.onUserCreated", e);
            } finally {
                closeJedis(jedis);
            }
        }
    }

    @Override
    public void onUserDestroyed(Record user) {
        Jedis jedis = getJedis();
        if (jedis != null) {
            try {
                jedis.publish("PlatformHook.onUserDestroyed", user.toString());
            } catch (Exception e) {
                L.error("PubHook.onUserDestroyed", e);
            } finally {
                closeJedis(jedis);
            }
        }
    }

    @Override
    public void onUserProfileChanged(Record changed) {
        Jedis jedis = getJedis();
        if (jedis != null) {
            try {
                jedis.publish("PlatformHook.onUserProfileChanged", changed.toString());
            } catch (Exception e) {
                L.error("PubHook.onUserProfileChanged", e);
            } finally {
                closeJedis(jedis);
            }
        }
    }

    @Override
    public void onFriendshipChange(Record changed) {
        Jedis jedis = getJedis();
        if (jedis != null) {
            try {
                jedis.publish("PlatformHook.onFriendshipChange", changed.toString());
            } catch (Exception e) {
                L.error("PubHook.onFriendshipChange", e);
            } finally {
                closeJedis(jedis);
            }
        }
    }

    @Override
    public void onSetFriendChange(Record changed) {
        Jedis jedis = getJedis();
        if (jedis != null) {
            try {
                jedis.publish("PlatformHook.onSetFriendChange", changed.toString());
            } catch (Exception e) {
                L.error("PubHook.onSetFriendChange", e);
            } finally {
                closeJedis(jedis);
            }
        }
    }
}
