package com.borqs.server.platform.mq.memcacheq;

import com.borqs.server.platform.cache.CacheElement;
import com.borqs.server.platform.cache.memcached.Memcached;
import com.borqs.server.platform.mq.MQ;
import com.borqs.server.platform.util.Initializable;
import org.apache.commons.collections.CollectionUtils;

import java.util.Arrays;
import java.util.List;


public abstract class MemcachedCompatible implements MQ, Initializable {
    private final Memcached memcached = new Memcached();

    protected MemcachedCompatible() {
    }

    public Memcached getMemcached() {
        return memcached;
    }

    public String getServer() {
        List<String> l = memcached.getServers();
        return CollectionUtils.isEmpty(l) ? null : l.get(0);
    }

    public void setServer(String server) {
        memcached.setServers(Arrays.asList(server));
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
    public void send(String queue, Object o) {
        memcached.put(CacheElement.forSet(queue, o));
    }

    @Override
    public Object receive(String queue) {
        CacheElement ce = memcached.get(queue);
        return ce != null ? ce.getValue() : null;
    }
}
