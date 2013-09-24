package com.borqs.server.sharelink;

import com.borqs.server.base.conf.Configuration;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Memcached {
    private static final Logger L = LoggerFactory.getLogger(Memcached.class);

    public void writeCache(String key, String value, String path) {
        int timeout = 60 * 24 * 30;    //miniutes,for 30 days
        Configuration conf = Configuration.loadFiles(path).expandMacros();

        MemcachedClient client;
        try {
            client = new XMemcachedClient(conf.checkGetString("platform.memcacheServerIpAddr"), Integer.valueOf(conf.checkGetString("platform.memcacheServerPortAddr")));// 默认端口
            client.set(key, timeout, value);
        } catch (TimeoutException e) {
            L.debug("write memcached error!:" + e.toString());
        } catch (InterruptedException e) {
            L.debug("write memcached error!:" + e.toString());
        } catch (MemcachedException e) {
            L.debug("write memcached error!:" + e.toString());
        } catch (IOException e) {
            L.debug("write memcached error!:" + e.toString());
        }
    }

    public String readCache(String key,String path) {
        Configuration conf = Configuration.loadFiles(path).expandMacros();
        MemcachedClient client;
        String value = "";
        try {
            client = new XMemcachedClient(conf.checkGetString("platform.memcacheServerIpAddr"), Integer.valueOf(conf.checkGetString("platform.memcacheServerPortAddr")));// 默认端口
            Object getSomeObject = client.get(key);
            if (getSomeObject != null) {
                value = getSomeObject.toString();
            }
            client.shutdown();
        } catch (TimeoutException e) {
            L.debug("write memcached error!:" + e.toString());
        } catch (InterruptedException e) {
            L.debug("write memcached error!:" + e.toString());
        } catch (MemcachedException e) {
            L.debug("write memcached error!:" + e.toString());
        } catch (IOException e) {
            L.debug("write memcached error!:" + e.toString());
        }
        return value;
    }
}
