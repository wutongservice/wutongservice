package com.borqs.server.platform.rpc;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.io.IOHelper;
import com.borqs.server.platform.service.Service;
import com.borqs.server.platform.util.ClassHelper;
import com.borqs.server.platform.util.Initializable;
import com.borqs.server.platform.util.NetAddress;
import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.NettyServer;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RPCService implements Service, Initializable, RPC {
    public static final int DEFAULT_PORT = 11003;

    private List<ServiceDelegate> delegates;
    private final Map<String, Invoker> invokers = new HashMap<String, Invoker>();
    private volatile Server server;
    private String address;

    public RPCService() {
    }

    public List<ServiceDelegate> getDelegates() {
        return delegates;
    }

    public void setDelegates(List<ServiceDelegate> delegates) {
        this.delegates = delegates;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public void init() throws Exception {
        if (CollectionUtils.isEmpty(delegates))
            return;


        for (ServiceDelegate delegate : delegates) {
            if (delegate == null)
                continue;
            invokers.put(delegate.getCategory(), new Invoker(ClassHelper.forName(delegate.getDelegateInterface()), delegate.getDelegateObject()));
        }
    }

    @Override
    public void destroy() {
        invokers.clear();
    }

    @Override
    public void start() {
        if (server != null)
            throw new IllegalStateException();

        Responder responder = new SpecificResponder(RPC.class, this);
        Server server = new NettyServer(responder, StringUtils.isBlank(address) ? new InetSocketAddress(DEFAULT_PORT): NetAddress.parseSocketAddress(address.trim()));
        server.start();
        this.server = server;
    }

    @Override
    public void stop() {
        if (server != null) {
            try {
                server.close();
                server.join();
            } catch (InterruptedException ignored) {
            } finally {
                server = null;
            }
        }
    }

    @Override
    public boolean isStarted() {
        return server != null;
    }

    @Override
    public ByteBuffer rpc(CharSequence category, CharSequence method, List<ByteBuffer> args) throws AvroRemoteException, RPCError {
        Invoker invoker = invokers.get(category.toString());
        if (invoker == null)
            throw makeRpcError(E.RPC, "No such category " + category.toString());

        try {
            return invoker.invoke(method.toString(), args);
        } catch (Throwable t) {
            if (t instanceof InvocationTargetException)
                t = ((InvocationTargetException) t).getTargetException();

            if (t instanceof ServerException)
                throw makeRpcError(((ServerException) t).getCode(), t.getMessage());
            else
                throw makeRpcError(E.RPC, t.getMessage());
        }
    }

    private static RPCError makeRpcError(int code, String message) {
        RPCError error = new RPCError();
        error.code = code;
        error.message = message;
        return error;
    }

    private static class Invoker {

        final Map<String, Method> methods = new HashMap<String, Method>();
        final Object delegateObject;

        private Invoker(Class delegateInterface, Object delegateObject) {
            for (Method m : delegateInterface.getMethods())
                methods.put(m.getName(), m);

            this.delegateObject = delegateObject;
        }

        public ByteBuffer invoke(String method, List<ByteBuffer> args) throws InvocationTargetException, IllegalAccessException {
            Method m = methods.get(method);
            if (m == null)
                throw new ServerException(E.RPC, "Not such method %s", method);

            Object[] argArr = new Object[args.size()];
            for (int i = 0; i < argArr.length; i++) {
                ByteBuffer byteBuff = args.get(i);
                argArr[i] = IOHelper.fromBytes(byteBuff.array());
            }
            Object r = m.invoke(delegateObject, argArr);
            return ByteBuffer.wrap(IOHelper.toBytes(r));
        }
    }
}
