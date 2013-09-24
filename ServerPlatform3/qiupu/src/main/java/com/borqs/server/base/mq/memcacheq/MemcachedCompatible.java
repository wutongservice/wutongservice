package com.borqs.server.base.mq.memcacheq;

import com.borqs.server.base.conf.ConfigurableBase;
import com.borqs.server.base.mq.MQ;
import com.borqs.server.base.mq.MQException;
import com.borqs.server.base.net.HostAndPort;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.ThreadUtils;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.exception.MemcachedException;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;


public abstract class MemcachedCompatible extends ConfigurableBase implements MQ, Initializable {
    private static final int DEFAULT_TIMEOUT = 1000 * 20;

    private String configKey;
    private volatile MemcachedClient client;
    private int receiveSleepInterval = 50; // ms

    protected MemcachedCompatible(String configKey) {
        this.configKey = StringUtils.removeEnd(configKey, ".");
    }

    public int getReceiveSleepInterval() {
        return receiveSleepInterval;
    }

    public void setReceiveSleepInterval(int receiveSleepInterval) {
        this.receiveSleepInterval = receiveSleepInterval;
    }

    @Override
    public void init() {
        if (client != null)
            throw new IllegalStateException();

        String server = getConfig().getString(configKey, null);
        MemcachedClientBuilder builder = new XMemcachedClientBuilder(Arrays.asList(HostAndPort.parse(server).toSocketAddress()));
        try {
            client = builder.build();
        } catch (IOException e) {
            throw new MQException(e);
        }
    }

    @Override
    public void destroy() {
        if (client != null) {
            try {
                client.shutdown();
            } catch (Exception e) {
                throw new MQException(e);
            } finally {
                client = null;
            }
        }
    }

    @Override
    public void send(String queue, String o) {
        try {
            client.set(queue, 0, o, DEFAULT_TIMEOUT);
        } catch (Exception e) {
            throw new MQException(e, "Send error");
        }
    }

    @Override
    public String receive(String queue) {
        try {
            return (String) client.get(queue, DEFAULT_TIMEOUT);
        } catch (Exception e) {
            throw new MQException(e, "Receive error");
        }
    }

    @Override
    public String receiveBlocked(String queue) {
        for (; ; ) {
            String r = receive(queue);
            if (r != null)
                return r;

            ThreadUtils.sleep(receiveSleepInterval);
        }
    }
}
