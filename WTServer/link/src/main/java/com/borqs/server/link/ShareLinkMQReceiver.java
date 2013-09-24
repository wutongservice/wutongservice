package com.borqs.server.link;

import com.borqs.server.base.ResponseError;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.memcache.XMemcached;
import com.borqs.server.base.mq.MQ;
import com.borqs.server.base.mq.MQCollection;
import com.borqs.server.base.util.ProcessUtils;
import com.borqs.server.base.util.ThreadUtils;
import com.borqs.server.base.util.email.ThreadPoolManager;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.base.web.JettyServer;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.shorturl.ShortUrlLogic;
import com.borqs.server.wutong.stream.StreamImpl;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;

import java.io.File;
import java.io.IOException;

public class ShareLinkMQReceiver {
    private static final Logger L = Logger.getLogger(ShareLinkMQReceiver.class);



    public static void main(String[] args) throws IOException, InterruptedException {
        try {
            L.debug(null,"mq receiver start");
            String confPath = "/home/zhengwei/workWT/dist-r3-distribution/etc/test.config.properties";
            //String confPath = "F:\\work\\refactProduct\\Dist\\src\\main\\etc\\test.config.properties";
//            String confPath = "/home/b516/BorqsServerPlatformWT/Dist/src/main/etc/test.config.properties";
            if ((args != null) && (args.length > 0)) {
                confPath = args[0];
            }
            GlobalConfig.loadFiles(confPath);


            MQCollection.initMQs();
            MQ mq = MQCollection.getMQ("platform");
            XMemcached mc = new XMemcached();
            mc.init();
            //pid
            String pidDirStr = FileUtils.getUserDirectoryPath() + "/.bpid";
            File pidDir = new File(pidDirStr);
            if (!pidDir.exists()) {
                FileUtils.forceMkdir(pidDir);
            }
            ProcessUtils.writeProcessId(pidDirStr + "/link_mq_receiver.pid");
            StreamImpl stream = new StreamImpl();
            stream.init();

            int errorCount = 0;
            while (true) {
                try {

                    String json = mq.receiveBlocked("link");
                    JsonNode jn = JsonUtils.parse(json);
                    String post_id = jn.path("post_id").getTextValue();
                    String url = jn.path("url").getTextValue();

                    //context cols
                    long viewerId = jn.path("viewerId").getLongValue();
                    String app = jn.path("app").getTextValue();
                    String ua = jn.path("ua").getTextValue();
                    String location = jn.path("location").getTextValue();
                    String language = jn.path("language").getTextValue();
                    Context ctx = new Context();
                    ctx.setViewerId(viewerId);
                    ctx.setAppId(app);
                    ctx.setUa(ua);
                    ctx.setLocation(location);
                    ctx.setLanguage(language);

                    final String METHOD = "receivedLinkShare";
                    L.traceStartCall(ctx, METHOD, json, post_id, url);

                    String attachment = "";
                    try {
                        attachment = mc.readCache(url);
                    } catch (Exception e) {
                        L.warn(ctx,e,"Read url from cache error");
                    }

                    L.debug(ctx,"find cache attachment=:" + attachment);
                    Configuration conf = GlobalConfig.get();
                    if (attachment.isEmpty()) {
                        ThreadPoolManager.getThreadPool().dispatch(createTask(conf, ctx, url, mc, post_id));
                    } else {
                        stream.updateAttachment(ctx, post_id, attachment);
                    }
                    L.traceEndCall(ctx, METHOD);
                    errorCount = 0;
                } catch (Exception e) {
                    if (errorCount < 50) {
                        L.error(null,e,"share link loop error(MQ)");
                        errorCount++;
                    } else {
                        ThreadUtils.sleep(1000);
                    }
                    try {
                        mq.destroy();
                        mq.init();
                    } catch (Exception e1) {
                        L.error(null,e1,"share link MQ restart(destroy) error");
                    }
                }
            }
        } finally {
            MQCollection.destroyMQs();
            try {
                ThreadPoolManager.getThreadPool().stop();
            } catch (Exception e) {
                L.warn(null,e,"ThreadPoolManager stop error");
            }
        }
    }

    private static Runnable createTask(final Configuration conf,final Context ctx,final String url, final XMemcached mc, final String post_id) {
        return new Runnable() {
            public void run() {
                RecordSet recs = new RecordSet();
                try {
                    LinkShare s = new LinkShare(conf);
//                    L.debug(ctx,"share link createTask start");
                    if (url.contains("z/v2mIRf") || url.contains("search?q=com.borqs.qiupu")) {
                        recs.add(returnSearch(url));
                    } else {
                        recs = s.shareLinkTaskIn(ctx, url);
                    }
                    L.debug(ctx,"share link received recs="+recs.toString(false, false));
                    if (recs.size()>0 && recs.getFirstRecord().getString("description").length() > 0) {
                        if (s.ifImgUrl(recs.getFirstRecord().getString("url"))) {
                            recs.getFirstRecord().put("description","");
                        }
                        StreamImpl stream = new StreamImpl();
                        stream.init();
//                        L.debug(ctx,"share link start to update attachments="+recs.toString(false, false));
                        stream.updateAttachment(ctx, post_id, recs.toString(false,false));
                        try {
                            mc.writeCache(url, recs.toString(false, false));
                        } catch (Exception e) {
                            L.warn(ctx, e, "write XMemcached error");
                        }
                    }
                    L.debug(ctx,"share link createTask end");
                } catch (Exception e) {
                }
            }
        };
    }

    private static Record returnSearch(String url) throws ResponseError {
        Record out_rec = new Record();
        ShortUrlLogic shortUrlLogic = GlobalLogics.getShortUrl();
        String ShortUrl = url.contains("z/v2mIRf") ? url : shortUrlLogic.generalShortUrl(url);
        out_rec.put("url", ShortUrl);
        out_rec.put("host", "api.borqs.com");
        out_rec.put("title", "Wu Tong");
        out_rec.put("description", "梧桐 是由北京播思软件技术有限公司2011年重磅推出的一款集应用管理、应用商店、社交网络、数据备份等多功能于一体的Android客户端软件。提供了超大数量的本地Android中文应用软件，支持数以万计的资源搜索与直接下载，是您的手机应用最佳伴侣。稳定可靠的应用数据备份和恢复功能让您不必手机丢失或换手机而导致的数据丢失而烦恼。提供社交网络功能，随时随地分享您的应用和心得。".toString());
        out_rec.put("img_url", "http://apps.borqs.com/images/dashboard.jpg");
        out_rec.put("many_img_url", "http://apps.borqs.com/images/dashboard.jpg");
        return out_rec;
    }
    public static class ServerLifeCycle implements JettyServer.LifeCycle {
            @Override
            public void before() throws Exception {
                GlobalLogics.init();
            }

            @Override
            public void after() throws Exception {
                GlobalLogics.destroy();
            }
        }
}