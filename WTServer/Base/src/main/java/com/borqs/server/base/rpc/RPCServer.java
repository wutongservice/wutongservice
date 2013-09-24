package com.borqs.server.base.rpc;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.net.HostAndPort;
import com.borqs.server.base.util.ClassUtils2;
import org.apache.avro.ipc.NettyServer;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.apache.commons.lang.ArrayUtils;

import java.net.InetSocketAddress;

public class RPCServer {
    public static void run(Configuration conf) {
        RPCService service = (RPCService) ClassUtils2.newInstance(conf.getString("service.class", null));
        InetSocketAddress bindAddr = HostAndPort.parseSocketAddress(conf.getString("service.address", null));
        try {
            service.init();
            Responder resp = new SpecificResponder(service.getInterface(), service.getImplement());
            NettyServer server = new NettyServer(resp, bindAddr);
            try {
                server.start();
                server.join();
            } catch (InterruptedException ignored) {
            } finally {
                server.close();
            }
        } finally {
            service.destroy();
        }
    }

    public static void printHelp() {
        System.out.printf("%s -c configPath1 [-c configPath2 ...]\n", RPCServer.class.getName());
    }

    public static void main(String[] args) {
        if (ArrayUtils.contains(args, "-h")) {
            printHelp();
            return;
        }

        Configuration conf = Configuration.loadArgs(args);
        conf.expandMacros();
        run(conf);
    }
}
