package com.borqs.server.platform.rpc;


import com.borqs.server.platform.util.NetAddress;
import org.apache.avro.ipc.NettyTransceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.Validate;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.List;

public class SimpleRpcProxy extends RPCProxy {
    public SimpleRpcProxy() {
    }

    @Override
    public void init() throws Exception {
    }

    @Override
    public void destroy() {
    }

    @Override
    public Object proxy(Class proxyClass, String category, List<String> addresses) {
        Validate.notNull(proxyClass);
        Validate.isTrue(CollectionUtils.isNotEmpty(addresses));
        return Proxy.newProxyInstance(SimpleRpcProxy.class.getClassLoader(), new Class[]{proxyClass}, new SimpleHandler(category, addresses));
    }

    private static class SimpleHandler extends Handler {
        SimpleHandler(String category, List<String> addresses) {
            super(category, addresses);
        }

        @Override
        protected TransceiverAndInterface getTransceiverAndInterface(String address) throws Exception {
            TransceiverAndInterface tai = new TransceiverAndInterface();
            tai.address = address;
            tai.transceiver = new NettyTransceiver(NetAddress.parseSocketAddress(address));
            tai.iface = SpecificRequestor.getClient(RPC.class, tai.transceiver);
            return tai;
        }

        @Override
        protected void closeTransceiverAndInterface(TransceiverAndInterface tai) throws IOException {
            tai.transceiver.close();
        }
    }
}
