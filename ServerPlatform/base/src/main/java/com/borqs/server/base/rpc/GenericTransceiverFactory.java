package com.borqs.server.base.rpc;


import com.borqs.server.base.conf.ConfigurableBase;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.net.HostAndPort;
import com.borqs.server.base.util.ClassUtils2;
import com.borqs.server.base.util.Initializable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.avro.ipc.LocalTransceiver;
import org.apache.avro.ipc.NettyTransceiver;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.net.InetSocketAddress;
import java.util.*;

public class GenericTransceiverFactory extends TransceiverFactory implements Initializable {
    private final Map<Class, TransceiverCreator> transCreators = new HashMap<Class, TransceiverCreator>();

    public GenericTransceiverFactory() {
    }

    @Override
    public synchronized void setConfig(Configuration conf) {
        super.setConfig(conf);
        final String TF = "transceivers";
        for (String key : conf.keySet()) {
            if (!key.startsWith(TF + "."))
                continue;

            String ifaceClassName = StringUtils.substringAfter(key, ".").trim();
            if (!ClassUtils2.classExists(ifaceClassName))
                throw new RPCException("Transceiver Factory can't find interface class '%s'", ifaceClassName);

            Class ifaceClass = ClassUtils2.forName(ifaceClassName);
            String implAddrs = conf.getString(key, "").trim();
            if (ClassUtils2.classExists(implAddrs)) {
                transCreators.put(ifaceClass, new LocalTransceiverCreator(ifaceClass, ClassUtils2.newInstance(implAddrs)));
            } else {
                transCreators.put(ifaceClass, new NettyTransceiverCreator(implAddrs));
            }

            for (TransceiverCreator tc : transCreators.values())
                tc.setConfig(conf);
        }
    }

    @Override
    public void init() {
        for (TransceiverCreator tc : transCreators.values())
            tc.init();
    }

    @Override
    public synchronized void destroy() {
        for (TransceiverCreator tc : transCreators.values())
            tc.destroy();

        transCreators.clear();
    }

    @Override
    public Transceiver getTransceiver(Class ifaceClass) {
        Validate.notNull(ifaceClass);
        TransceiverCreator transCreator;
        synchronized (this) {
            transCreator = transCreators.get(ifaceClass);
        }
        if (transCreator == null)
            throw new RPCException("Can't find transceiver for interface '%s'", ifaceClass.getName());

        return transCreator.create();
    }

    protected static abstract class TransceiverCreator extends ConfigurableBase implements Initializable {
        abstract Transceiver create();
    }

    protected static class LocalTransceiverCreator extends TransceiverCreator {
        final Object impl;
        final SpecificResponder responder;

        public LocalTransceiverCreator(Class ifaceClass, Object impl) {
            this.impl = impl;
            responder = new SpecificResponder(ifaceClass, impl);
        }

        @Override
        Transceiver create() {
            return new LocalTransceiver(responder);
        }

        @Override
        public void setConfig(Configuration conf) {
            if (impl instanceof ConfigurableBase)
                ((ConfigurableBase) impl).setConfig(conf);
        }

        @Override
        public void init() {
            if (impl instanceof Initializable)
                ((Initializable) impl).init();
        }

        @Override
        public void destroy() {
            if (impl instanceof Initializable)
                ((Initializable) impl).destroy();
        }
    }

    protected static class NettyTransceiverCreator extends TransceiverCreator {
        final List<InetSocketAddress> addresses;

        public NettyTransceiverCreator(String addrs) {
            String[] addrArr = StringUtils.split(addrs, ",");
            addresses = new ArrayList<InetSocketAddress>();
            for (String addr : addrArr) {
                if (StringUtils.isBlank(addr))
                    continue;

                addresses.add(HostAndPort.parseSocketAddress(addr.trim()));
            }
        }

        @Override
        Transceiver create() {
            Random rand = new Random();
            int index = rand.nextInt(addresses.size());
            InetSocketAddress sockAddr = addresses.get(index);
            try {
                return new NettyTransceiver(sockAddr);
            } catch (IOException ex) {
                return null;
            }
        }

        @Override
        public void setConfig(Configuration conf) {
        }

        @Override
        public void init() {
        }

        @Override
        public void destroy() {
        }
    }

}
