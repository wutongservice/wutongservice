package com.borqs.server.base.net;


import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.net.InetSocketAddress;


public class HostAndPort {
    public final String host;
    public final int port;

    public HostAndPort(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public InetSocketAddress toSocketAddress() {
        return host != null ? new InetSocketAddress(host, port) : new InetSocketAddress(port);
    }

    public static InetSocketAddress parseSocketAddress(String s) {
        return parse(s).toSocketAddress();
    }

    public static HostAndPort parse(String s) {
        Validate.notNull(s);
        String[] ss = StringUtils.split(s, ":", 2);
        ss[0] = ss[0].trim();
        ss[1] = ss[1].trim();
        return new HostAndPort(ss[0].equals("*") ? null : ss[0], Integer.parseInt(ss[1]));
    }
}
