package com.borqs.server.platform.util;


import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;


public class NetAddress {
    public final String host;
    public final int port;

    public NetAddress(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public InetSocketAddress toSocketAddress() {
        return host != null ? new InetSocketAddress(host, port) : new InetSocketAddress(port);
    }

    public static List<InetSocketAddress> parseSocketAddresses(List<String> ss) {
        ArrayList<InetSocketAddress> l = new ArrayList<InetSocketAddress>();
        for (String s : ss)
            l.add(parseSocketAddress(s));
        return l;
    }

    public static InetSocketAddress parseSocketAddress(String s) {
        return parse(s).toSocketAddress();
    }

    public static NetAddress parse(String s) {
        Validate.notNull(s);
        String[] ss = StringUtils.split(s, ":", 2);
        ss[0] = ss[0].trim();
        ss[1] = ss[1].trim();
        return new NetAddress(ss[0].equals("*") ? null : ss[0], Integer.parseInt(ss[1]));
    }
}
