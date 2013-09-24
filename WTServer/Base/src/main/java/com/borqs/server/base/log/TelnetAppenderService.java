package com.borqs.server.base.log;


import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import com.borqs.server.base.io.Charsets;
import com.borqs.server.base.net.HostAndPort;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class TelnetAppenderService {
    private static TelnetAppenderService INSTANCE = new TelnetAppenderService();

    static final ChannelGroup allChannels = new DefaultChannelGroup("log-telnet-append-server");
    static final Map<Integer, Filter> filters = new ConcurrentHashMap<Integer, Filter>();

    private String address = "*:11100";

    private volatile ChannelFactory channelFactory = null;


    private TelnetAppenderService() {
    }

    public static TelnetAppenderService getInstance() {
        return INSTANCE;
    }

    public void append(ILoggingEvent le, Layout<ILoggingEvent> layout) {
        HashMap<Integer, Filter> m = new HashMap<Integer, Filter>(filters);
        for (Map.Entry<Integer, Filter> e : m.entrySet()) {
            Channel channel = allChannels.find(e.getKey());
            if (channel == null)
                continue;

            Filter filter = e.getValue();
            if (filter.filter(le)) {
                String line = layout.doLayout(le);
                channel.write(line + "\n");
            }
        }
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void start() {
        if (channelFactory != null)
            throw new IllegalStateException("Telnet appender server is running");

        channelFactory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);
        bootstrap.setPipelineFactory(new ServerPipelineFactory());
        Channel channel = bootstrap.bind(HostAndPort.parseSocketAddress(address));
        allChannels.add(channel);
    }

    public void stop() {
        if (channelFactory != null) {
            try {
                ChannelGroupFuture future = allChannels.close();
                future.awaitUninterruptibly();
                channelFactory.releaseExternalResources();
            } finally {
                channelFactory = null;
            }
        }
    }

    public boolean isStarted() {
        return channelFactory != null;
    }

    private class ServerHandler extends SimpleChannelUpstreamHandler {
        public static final String CMD_EXIT = "exit";
        public static final String CMD_QUIT = "quit";
        public static final String CMD_ON = "on";
        public static final String CMD_OFF = "off";

        @Override
        public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            allChannels.add(e.getChannel());
            filters.put(e.getChannel().getId(), new Filter());
        }

        @Override
        public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            filters.remove(e.getChannel().getId());
            allChannels.remove(e.getChannel());
        }

        @Override
        public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            filters.remove(e.getChannel().getId());
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            String msg = StringUtils.trimToEmpty((String) e.getMessage());
            if (CMD_EXIT.equalsIgnoreCase(msg) || CMD_QUIT.equalsIgnoreCase(msg)) {
                e.getChannel().close();
            } else if (CMD_ON.equalsIgnoreCase(msg)) {
                Filter f = filters.get(e.getChannel().getId());
                if (f != null)
                    f.enabled = true;
            } else if (CMD_OFF.equalsIgnoreCase(msg)) {
                Filter f = filters.get(e.getChannel().getId());
                if (f != null)
                    f.enabled = false;
            }
        }
    }

    private class ServerPipelineFactory implements ChannelPipelineFactory {
        @Override
        public ChannelPipeline getPipeline() throws Exception {
            ChannelPipeline pipeline = Channels.pipeline();
            pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
            pipeline.addLast("decoder", new StringDecoder(Charsets.DEFAULT_CHARSET));
            pipeline.addLast("encoder", new StringEncoder(Charsets.DEFAULT_CHARSET));
            pipeline.addLast("handler", new ServerHandler());
            return pipeline;
        }
    }

    private static class Filter {
        volatile boolean enabled = true;

        public boolean filter(ILoggingEvent e) {
            return enabled;
        }
    }
}
