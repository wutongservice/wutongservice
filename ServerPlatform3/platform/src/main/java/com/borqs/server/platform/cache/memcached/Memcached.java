package com.borqs.server.platform.cache.memcached;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.cache.AbstractCache;
import com.borqs.server.platform.cache.CacheElement;
import com.borqs.server.platform.io.IOHelper;
import com.borqs.server.platform.util.Initializable;
import com.borqs.server.platform.util.NetAddress;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.Validate;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class Memcached extends AbstractCache implements Initializable {
    private List<String> servers;
    private List<Integer> weights;
    private volatile MemcachedClient client;
    private int timeout = 20 * 1000; // 20 seconds
    private Transcoder transcoder;

    public Memcached() {
    }

    public String getServer() {
        return CollectionUtils.isNotEmpty(servers) ? servers.get(0) : null;
    }

    public void setServer(String server) {
        servers = Arrays.asList(server);
    }

    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        this.servers = servers;
    }

    public List<Integer> getWeights() {
        return weights;
    }

    public void setWeights(List<Integer> weights) {
        this.weights = weights;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public Transcoder getTranscoder() {
        return transcoder;
    }

    public void setTranscoder(Transcoder transcoder) {
        this.transcoder = transcoder;
    }

    public MemcachedClient getMemcached() {
        return client;
    }

    @Override
    public void init() throws Exception {
        if (client != null)
            throw new IllegalStateException("The server is running");

        if (CollectionUtils.isEmpty(servers))
            throw new IllegalStateException("Need servers");

        if (CollectionUtils.isNotEmpty(weights))
            Validate.isTrue(servers.size() == weights.size());


        List<InetSocketAddress> addrs = NetAddress.parseSocketAddresses(servers);
        MemcachedClientBuilder builder;
        if (CollectionUtils.isNotEmpty(weights)) {
            builder = new XMemcachedClientBuilder(addrs, ArrayUtils.toPrimitive(weights.toArray(new Integer[weights.size()])));
        } else {
            builder = new XMemcachedClientBuilder(addrs);
        }

        client = builder.build();
        client.setTranscoder(transcoder != null ? transcoder : new SerializingTranscoder(1024 * 1024 * 48));
    }

    @Override
    public void destroy() {
        if (client != null) {
            try {
                client.shutdown();
            } catch(Exception ignored) {
            } finally {
                client = null;
            }
        }
    }


    @Override
    public void put(CacheElement value) {
        Validate.notNull(value);
        try {
            client.set(value.getKey(), value.hasExpiry() ? (int)value.getExpirySeconds() : 0, IOHelper.toBytes(value.getValue()), timeout);
        } catch (TimeoutException e) {
            throw new ServerException(E.CACHE, e);
        } catch (InterruptedException e) {
            throw new ServerException(E.CACHE, e);
        } catch (MemcachedException e) {
            throw new ServerException(E.CACHE, e);
        }
    }

    @Override
    public CacheElement get(String key) {
        try {
            byte[] bytes = client.get(key, timeout);
            return CacheElement.forResult(key, bytes != null ? IOHelper.fromBytes(bytes) : null);
        } catch (TimeoutException e) {
            throw new ServerException(E.CACHE, e);
        } catch (InterruptedException e) {
            throw new ServerException(E.CACHE, e);
        } catch (MemcachedException e) {
            throw new ServerException(E.CACHE, e);
        }
    }

    @Override
    public Collection<CacheElement> gets(Collection<String> keys) {
        try {
            ArrayList<CacheElement> ces = new ArrayList<CacheElement>();
            Map<String, byte[]> m = client.get(keys, timeout);
            for (Map.Entry<String, byte[]> e : m.entrySet()) {
                byte[] bytes = e.getValue();
                CacheElement ce = CacheElement.forResult(e.getKey(), bytes != null ? IOHelper.fromBytes(bytes) : null);
                ces.add(ce);
            }
            return ces;
        } catch (TimeoutException e) {
            throw new ServerException(E.CACHE, e);
        } catch (InterruptedException e) {
            throw new ServerException(E.CACHE, e);
        } catch (MemcachedException e) {
            throw new ServerException(E.CACHE, e);
        }
    }


    @Override
    public void delete(String key) {
        try {
            client.delete(key, (long)timeout);
        } catch (TimeoutException e) {
            throw new ServerException(E.CACHE, e);
        } catch (InterruptedException e) {
            throw new ServerException(E.CACHE, e);
        } catch (MemcachedException e) {
            throw new ServerException(E.CACHE, e);
        }
    }
}
