package com.borqs.server.platform.rpc;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.util.NetAddress;
import org.apache.avro.ipc.NettyTransceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

import java.lang.reflect.Proxy;
import java.util.List;

public class PooledRpcProxy extends RPCProxy {
    private final GenericKeyedObjectPool pool = new GenericKeyedObjectPool(new TransceiverAndInterfaceFactory());

    public PooledRpcProxy() {
    }

    public int getMaxActive() {
        return pool.getMaxActive();
    }

    public void setMaxActive(int maxActive) {
        pool.setMaxActive(maxActive);
    }

    public int getMaxTotal() {
        return pool.getMaxTotal();
    }

    public void setMaxTotal(int maxTotal) {
        pool.setMaxTotal(maxTotal);
    }

    public byte getWhenExhaustedAction() {
        return pool.getWhenExhaustedAction();
    }

    public void setWhenExhaustedAction(byte whenExhaustedAction) {
        pool.setWhenExhaustedAction(whenExhaustedAction);
    }

    public long getMaxWait() {
        return pool.getMaxWait();
    }

    public void setMaxWait(long maxWait) {
        pool.setMaxWait(maxWait);
    }

    public int getMaxIdle() {
        return pool.getMaxIdle();
    }

    public void setMaxIdle(int maxIdle) {
        pool.setMaxIdle(maxIdle);
    }

    public void setMinIdle(int poolSize) {
        pool.setMinIdle(poolSize);
    }

    public int getMinIdle() {
        return pool.getMinIdle();
    }

    public boolean getTestOnBorrow() {
        return pool.getTestOnBorrow();
    }

    public void setTestOnBorrow(boolean testOnBorrow) {
        pool.setTestOnBorrow(testOnBorrow);
    }

    public boolean getTestOnReturn() {
        return pool.getTestOnReturn();
    }

    public void setTestOnReturn(boolean testOnReturn) {
        pool.setTestOnReturn(testOnReturn);
    }

    public long getTimeBetweenEvictionRunsMillis() {
        return pool.getTimeBetweenEvictionRunsMillis();
    }

    public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        pool.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
    }

    public int getNumTestsPerEvictionRun() {
        return pool.getNumTestsPerEvictionRun();
    }

    public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        pool.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
    }

    public long getMinEvictableIdleTimeMillis() {
        return pool.getMinEvictableIdleTimeMillis();
    }

    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        pool.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
    }

    public boolean getTestWhileIdle() {
        return pool.getTestWhileIdle();
    }

    public void setTestWhileIdle(boolean testWhileIdle) {
        pool.setTestWhileIdle(testWhileIdle);
    }

    public boolean getLifo() {
        return pool.getLifo();
    }

    public void setLifo(boolean lifo) {
        pool.setLifo(lifo);
    }

    @Override
    public void init() throws Exception {
    }

    @Override
    public void destroy() {
        try {
            pool.close();
        } catch (Exception e) {
            throw new ServerException(E.RPC, e);
        }
    }

    @Override
    public Object proxy(Class proxyClass, String category, List<String> addresses) {
        Validate.notNull(proxyClass);
        Validate.isTrue(CollectionUtils.isNotEmpty(addresses));
        return Proxy.newProxyInstance(SimpleRpcProxy.class.getClassLoader(), new Class[]{proxyClass}, new PooledHandler(category, addresses));
    }

    private static class TransceiverAndInterfaceFactory implements KeyedPoolableObjectFactory {
        @Override
        public Object makeObject(Object key) throws Exception {
            String address = key.toString();
            TransceiverAndInterface tai = new TransceiverAndInterface();
            tai.address = key.toString();
            tai.transceiver = new NettyTransceiver(NetAddress.parseSocketAddress(address));
            tai.iface = SpecificRequestor.getClient(RPC.class, tai.transceiver);
            return tai;
        }

        @Override
        public void destroyObject(Object key, Object obj) throws Exception {
            TransceiverAndInterface tai = (TransceiverAndInterface)obj;
            tai.transceiver.close();
        }

        @Override
        public boolean validateObject(Object key, Object obj) {
            TransceiverAndInterface tai = (TransceiverAndInterface)obj;
            return tai.transceiver.isConnected();
        }

        @Override
        public void activateObject(Object key, Object obj) throws Exception {
        }

        @Override
        public void passivateObject(Object key, Object obj) throws Exception {
        }
    }

    private class PooledHandler extends Handler {
        private PooledHandler(String category, List<String> addresses) {
            super(category, addresses);
        }

        @Override
        protected TransceiverAndInterface getTransceiverAndInterface(String address) throws Exception {
            return (TransceiverAndInterface)pool.borrowObject(address);
        }

        @Override
        protected void closeTransceiverAndInterface(TransceiverAndInterface tai) throws Exception {
            pool.returnObject(tai.address, tai);
        }
    }
}
