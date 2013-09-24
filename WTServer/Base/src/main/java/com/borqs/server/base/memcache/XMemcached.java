package com.borqs.server.base.memcache;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.StringUtils2;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class XMemcached implements Initializable {
    private static final Logger L = Logger.getLogger(XMemcached.class);
    private MemcachedClient client;
    private int timeout = 60 * 24 * 30;

    @Override
    public void init() {
        Configuration conf = GlobalConfig.get();
//        Configuration conf = getConfig();
        try {
            client = new XMemcachedClient(conf.getString("platform.memcacheServerIpAddr", "localhost"), Integer.valueOf(conf.getString("platform.memcacheServerPortAddr", "11211")));
        } catch (IOException e) {
            L.debug(null, "memcached create error!:" + e.toString());
        }
    }

    @Override
    public void destroy() {
        try {
            client.shutdown();
        } catch (IOException e) {
            L.debug(null, "memcached shutdown error!:" + e.toString());
        }
    }

    public void flushAll() {
        try {
            client.flushAll();
        } catch (Exception e) {
            L.debug(null, "memcached shutdown error!:" + e.toString());
        }
    }

    public void writeCache(String key, String value) {
        try {
            client.set(key, timeout, StringUtils2.compress(value));
        } catch (Exception e) {
            L.debug(null, "write  memcached error!:" + e.toString());
        }
    }

    public String readCache(String key) {
        String value = "";
        try {
            Object getSomeObject = client.get(key);
            if (getSomeObject != null) {
                value = StringUtils2.uncompress(getSomeObject.toString());
            }
        } catch (Exception e) {
            L.debug(null, "read memcached error!:" + e.toString());
        }
        return value;
    }

    public void deleteCache(String key) {
        try {
            if (client.get(key) != null)
                client.delete(key);
        } catch (Exception e) {
            L.debug(null, "delete memcached error!:" + e.toString());
        }
    }

    public void replaceCache(String key, String value) {
        try {
            client.replace(key, 0, StringUtils2.compress(value));
        } catch (Exception e) {
            L.debug(null, "replace memcached error!:" + e.toString());
        }
    }

    public void replaceRecordCache(String key, Record value) {
        try {
            client.replace(key, 0, value);
        } catch (Exception e) {
            L.debug(null, "replace memcached error!:" + e.toString());
        }
    }

    public void writeRecordCache(String key, Record value) {
        try {
            client.set(key, timeout, value);
        } catch (Exception e) {
            L.debug(null, "write  memcached error!:" + e.toString());
        }
    }

    public Map<String, Record> readMultiRecordCache(Collection<String> collection) throws MemcachedException, TimeoutException, InterruptedException {
        Map<String, Record> map = client.get(collection);
        return map;
    }

    public Map<String, String> readMultiCache(Collection<String> collection) throws MemcachedException, TimeoutException, InterruptedException {
        Map<String, String> map = client.get(collection);
        return map;
    }
}
