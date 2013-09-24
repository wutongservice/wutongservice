package com.borqs.server.base.redis;

import com.borqs.server.base.conf.ConfigurableBase;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.StringUtils2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;

public class Redis extends ConfigurableBase implements Initializable {
    private static final Logger L = LoggerFactory.getLogger(Redis.class);
    private Jedis jedis;
    private int timeout = 60 * 24 * 30;

    public String ListKey(String key) {
        return key;
    }

    @Override
    public void init() {
        Configuration conf = getConfig();
        jedis = new Jedis(conf.getString("platform.redisServerAddr", "localhost"), 6379);
    }

    @Override
    public void destroy() {
        jedis.quit();
        jedis.shutdown();
    }

    public void writeRedis(String key, String value) {
        try {
            jedis.set(key, StringUtils2.compress(value));
            jedis.move(key,1);
        } catch (Exception e) {
            L.debug("write Redis error!:" + e.toString());
        } finally {
            jedis.quit();
        }
    }

    public String readRedis(String key) {
        String value = "";
        try {
            value = jedis.get(key);
            if (value.length() > 0) {
                value = StringUtils2.uncompress(value);
            } else {
                jedis.select(1);
                jedis.get(key);
                if (value.length() > 0) {
                    value = StringUtils2.uncompress(value);
                }
            }
        } catch (Exception e) {
            L.debug("read Redis error!:" + e.toString());
        } finally {
            jedis.quit();
        }
        return value;
    }

    public boolean deleteKey(String key) {
        boolean b = true;
        try {
            b = jedis.del(ListKey(key)) > 0;
        } catch (Exception e) {
            L.debug("delete redisKey error!:" + e.toString());
        } finally {
            jedis.quit();
        }
        jedis.quit();
        return b;
    }

    public boolean writeToList(String key, String value) {
        try {
            jedis.lpushx(ListKey(key), value);
            List<String> stringList = readInList(key);
            if (stringList.size() > 500) {
                for (int i = stringList.size() - 1; i > 500; i--) {
                    String v = stringList.get(i).toString();
                    jedis.lrem(ListKey(key), -1, v);
                }
            }
            jedis.quit();
        } catch (Exception e) {
            L.debug("writeToList error!:" + e.toString());
        } finally {
            jedis.quit();
        }
        return true;
    }


    public List<String> readInList(String key) {
        List<String> stringList = new ArrayList<String>();
        try {
            stringList = jedis.lrange(ListKey(key), 0, -1);
            for (String a : stringList) {
                a = StringUtils2.uncompress(a);
            }
        } catch (Exception e) {
            L.debug("readInList error!:" + e.toString());
        } finally {
            jedis.quit();
        }
        return stringList;
    }

    public boolean deleteFromList(String key, String value) {
        List<String> stringList = new ArrayList<String>();
        try {
            stringList = jedis.lrange(ListKey(key), 0, -1);
            for (int i = stringList.size() - 1; i >= 0; i--) {
                String a = StringUtils2.uncompress(stringList.get(i).toString());
                if (a.equals(value) && !value.equals(""))
                    jedis.lrem(ListKey(key), -1, a);
            }
        } catch (Exception e) {
            L.debug("deleteFromList error!:" + e.toString());
        } finally {
            jedis.quit();
        }
        return true;
    }
}
