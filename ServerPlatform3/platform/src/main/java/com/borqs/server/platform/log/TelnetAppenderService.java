package com.borqs.server.platform.log;


import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.service.Service;
import com.borqs.server.platform.util.NetAddress;
import com.borqs.server.platform.util.StringHelper;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class TelnetAppenderService implements Service {
    private static TelnetAppenderService INSTANCE = new TelnetAppenderService();

    static final ChannelGroup allChannels = new DefaultChannelGroup("log-telnet-append-server");
    static final Map<Integer, Filter> filters = new ConcurrentHashMap<Integer, TelnetAppenderService.Filter>();

    private String address = "*:11300";

    private volatile ChannelFactory channelFactory = null;


    private TelnetAppenderService() {
    }

    public static TelnetAppenderService getInstance() {
        return INSTANCE;
    }

    public void append(ILoggingEvent le, Layout<ILoggingEvent> layout) {
        if (!isStarted())
            return;

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

    @Override
    public void start() {
        if (channelFactory != null)
            throw new IllegalStateException("Telnet appender server is running");

        channelFactory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);
        bootstrap.setPipelineFactory(new ServerPipelineFactory());
        Channel channel = bootstrap.bind(NetAddress.parseSocketAddress(address));
        allChannels.add(channel);
    }

    @Override
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

    @Override
    public boolean isStarted() {
        return channelFactory != null;
    }

    private class ServerHandler extends SimpleChannelUpstreamHandler {
        public static final String CMD_EXIT = "exit";
        public static final String CMD_QUIT = "quit";
        public static final String CMD_ON = "on";
        public static final String CMD_OFF = "off";
        public static final String CMD_VIEW = "view";
        public static final String CMD_REMOTE = "remote";

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
            String msg = StringUtils.trimToEmpty((String)e.getMessage());
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
            } else if (StringUtils.startsWithIgnoreCase(msg, CMD_VIEW)) {
                Filter f = filters.get(e.getChannel().getId());
                if (f != null)
                    f.setViewers(StringUtils.removeStartIgnoreCase(msg, CMD_VIEW).trim());
            } else if (StringUtils.startsWithIgnoreCase(msg, CMD_REMOTE)) {
                Filter f = filters.get(e.getChannel().getId());
                if (f != null)
                    f.setRemotes(StringUtils.removeStartIgnoreCase(msg, CMD_REMOTE).trim());
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
        volatile boolean enabled = false;
        Set<String> viewers = new ConcurrentHashSet<String>();
        Set<String> remotes = new ConcurrentHashSet<String>();

        void setViewers(String viewerIds) {
            viewers.clear();
            viewers.addAll(StringHelper.splitList(viewerIds, ",", true));
        }

        void setRemotes(String remotes) {
            this.remotes.clear();
            this.remotes.addAll(StringHelper.splitList(remotes, ",", true));
        }

        public String getViewers() {
            return StringUtils.join(viewers, ",");
        }

        public String getRemotes() {
            return StringUtils.join(remotes, ",");
        }

        public boolean filter(ILoggingEvent e) {
            if (!enabled)
                return false;


            boolean r = true;
            if (!viewers.isEmpty()) {
                String viewer = e.getMdc().get(CsvLayout.KEY_VIEWER);
                r = viewers.contains(viewer);
            }

            if (!r)
                return false;

            if (!remotes.isEmpty()) {
                String remote = e.getMdc().get(CsvLayout.KEY_REMOTE);
                r = remotes.contains(remote);
            }
            return r;
        }
    }
}
