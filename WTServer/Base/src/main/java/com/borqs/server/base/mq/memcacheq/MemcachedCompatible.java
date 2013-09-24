package com.borqs.server.base.mq.memcacheq;

import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.mq.MQ;
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


public abstract class MemcachedCompatible implements MQ, Initializable {
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

        String server = GlobalConfig.get().getString(configKey, null);
        MemcachedClientBuilder builder = new XMemcachedClientBuilder(Arrays.asList(HostAndPort.parse(server).toSocketAddress()));
        try {
            client = builder.build();
        } catch (IOException e) {
            throw new ServerException(BaseErrors.PLATFORM_MQ_INIT_ERROR, e);
        }
    }

    @Override
    public void destroy() {
        if (client != null) {
            try {
                client.shutdown();
            } catch (Exception e) {
                throw new ServerException(BaseErrors.PLATFORM_MQ_DESTROY_ERROR, e);
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
            throw new ServerException(BaseErrors.PLATFORM_MQ_SEND_ERROR, e, "Send error");
        }
    }

    @Override
    public void send(String queue, Object o) {
        try {
            client.set(queue, 0, o, DEFAULT_TIMEOUT);
        } catch (Exception e) {
            throw new ServerException(BaseErrors.PLATFORM_MQ_SEND_ERROR, e, "Send error");
        }
    }

    @Override
    public String receive(String queue) {
        try {
            return (String) client.get(queue, DEFAULT_TIMEOUT);
        } catch (Exception e) {
            throw new ServerException(BaseErrors.PLATFORM_MQ_RECEIVE_ERROR, e, "Receive error");
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

    @Override
    public Object receiveBlockedObject(String queue) {
        for (; ; ) {
            Object r = null;
            try {
                r = client.get(queue, DEFAULT_TIMEOUT);
            } catch (TimeoutException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (MemcachedException e) {
                e.printStackTrace();
            }
            if (r != null)
                return r;
            ThreadUtils.sleep(receiveSleepInterval);
        }
    }
}
