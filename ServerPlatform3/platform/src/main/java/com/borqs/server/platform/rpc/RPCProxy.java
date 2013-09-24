package com.borqs.server.platform.rpc;


import com.borqs.server.ServerException;
import com.borqs.server.platform.io.IOHelper;
import com.borqs.server.platform.util.ClassHelper;
import com.borqs.server.platform.util.Initializable;
import com.borqs.server.platform.util.RandomHelper;
import org.apache.avro.ipc.Transceiver;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class RPCProxy implements Initializable {
    protected RPCProxy() {
    }

    public Object proxy(String proxyClassName, String category, String address) {
        Validate.notNull(address);
        return proxy(ClassHelper.forName(proxyClassName), category, Arrays.asList(address));
    }

    public Object proxy(String proxyClassName, String category, List<String> addresses) {
        return proxy(ClassHelper.forName(proxyClassName), category, addresses);
    }

    public Object proxy(Class proxyClass, String category, String address) {
        Validate.notNull(address);
        return proxy(proxyClass, category, Arrays.asList(address));
    }

    public abstract Object proxy(Class proxyClass, String category, List<String> addresses);

    protected static abstract class Handler implements InvocationHandler {
        private String category;
        private final List<String> addresses;

        public Handler(String category, List<String> addresses) {
            this.category = category;
            this.addresses = addresses;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            TransceiverAndInterface tai = null;
            try {
                String address = RandomHelper.randomSelect(addresses);
                tai = getTransceiverAndInterface(address);
                ArrayList<ByteBuffer> argBuffs = new ArrayList<ByteBuffer>(args.length);
                for (Object arg : args)
                    argBuffs.add(ByteBuffer.wrap(IOHelper.toBytes(arg)));
                ByteBuffer resultBuff = tai.iface.rpc(category, method.getName(), argBuffs);
                return IOHelper.fromBytes(resultBuff.array());
            } catch (RPCError e) {
                throw new ServerException(e.code, ObjectUtils.toString(e.message));
            } finally {
                if (tai != null)
                    closeTransceiverAndInterface(tai);
            }
        }


        protected abstract TransceiverAndInterface getTransceiverAndInterface(String address) throws Exception;
        protected abstract void closeTransceiverAndInterface(TransceiverAndInterface tai) throws Exception;
    }

    protected static class TransceiverAndInterface {
        String address;
        RPC iface;
        Transceiver transceiver;
    }
}
