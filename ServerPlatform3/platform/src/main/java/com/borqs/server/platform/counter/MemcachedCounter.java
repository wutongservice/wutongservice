package com.borqs.server.platform.counter;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.cache.memcached.Memcached;
import com.borqs.server.platform.util.Initializable;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.transcoders.StringTranscoder;
import org.apache.commons.lang.StringUtils;

import java.util.concurrent.TimeoutException;

public class MemcachedCounter extends AbstractCounter implements Initializable {
    private final Memcached memcached = new Memcached();

    public MemcachedCounter() {
        memcached.setTranscoder(new StringTranscoder());
    }

    public void setServer(String server) {
        memcached.setServer(server);
    }

    public String getServer() {
        return memcached.getServer();
    }

    public int getTimeout() {
        return memcached.getTimeout();
    }

    public void setTimeout(int timeout) {
        memcached.setTimeout(timeout);
    }

    @Override
    public void init() throws Exception {
        memcached.init();
    }

    @Override
    public void destroy() {
        memcached.destroy();
    }

    @Override
    public long increase(String key, long n, long init) {
        try {
            return memcached.getMemcached().incr(key, n, init, memcached.getTimeout());
        } catch (TimeoutException e) {
            throw new ServerException(E.CACHE, e);
        } catch (InterruptedException e) {
            throw new ServerException(E.CACHE, e);
        } catch (MemcachedException e) {
            throw new ServerException(E.CACHE, e);
        }
    }

    @Override
    public long get(String key) {
        try {
            String s = memcached.getMemcached().get(key, memcached.getTimeout());
            return StringUtils.isNotBlank(s) ? Long.parseLong(s) : 0;
        } catch (TimeoutException e) {
            throw new ServerException(E.CACHE, e);
        } catch (InterruptedException e) {
            throw new ServerException(E.CACHE, e);
        } catch (MemcachedException e) {
            throw new ServerException(E.CACHE, e);
        }
    }
}
