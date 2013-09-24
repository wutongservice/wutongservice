package com.borqs.server.sharelink;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.memcache.XMemcached;
import com.borqs.server.base.mq.MQ;
import com.borqs.server.base.mq.MQCollection;
import com.borqs.server.base.mq.MQException;
import com.borqs.server.base.rpc.GenericTransceiverFactory;
import com.borqs.server.base.util.ProcessUtils;
import com.borqs.server.base.util.email.ThreadPoolManager;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.service.platform.Platform;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ShareLinkMQReceiver {
    private static final Logger L = LoggerFactory.getLogger(ShareLinkMQReceiver.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        try {
            L.debug("ShareLinkMQReceiver begin");
            GenericTransceiverFactory tf = new GenericTransceiverFactory();

            String path = Constants.confPath;
            if (path.equals("") || path ==null)
                path = "/home/zhengwei/work2/dist/etc/test_web_server.properties";
            if ((args != null) && (args.length > 0)) {
                L.trace("ShareLinkMQReceiver args length: " + args.length);
                L.trace("ShareLinkMQReceiver arg: " + args[0]);
                path = args[0];
            }

            Configuration conf = Configuration.loadFiles(path).expandMacros();
            tf.setConfig(conf);
            tf.init();

            final Platform p = new Platform(tf);
            p.setConfig(conf);

            MQCollection.initMQs(conf);
            MQ mq = MQCollection.getMQ("platform");

            XMemcached mc = new XMemcached();
            mc.path = path;
            mc.init();

            //pid
            String pidDirStr = FileUtils.getUserDirectoryPath() + "/.bpid";
            File pidDir = new File(pidDirStr);
            if (!pidDir.exists()) {
                FileUtils.forceMkdir(pidDir);
            }
            ProcessUtils.writeProcessId(pidDirStr + "/link_mq_receiver.pid");

            while (true) {
                try {

                    String json = mq.receiveBlocked("link");
                    JsonNode jn = JsonUtils.parse(json);
                    String post_id = jn.path("post_id").getTextValue();
                    String url = jn.path("url").getTextValue();
                    String linkImagAddr = jn.path("linkImagAddr").getTextValue();

                    L.debug("receive link url=:" + url);

                    String attachment = "";
                    try {
                        attachment = mc.readCache(url);
                    } catch (Exception e) {
                        L.debug("Read url from cache error", e);
                    }

                    L.debug("attachment=:" + attachment + "post_id=:" + post_id);

                    if (attachment.isEmpty()) {
                        ThreadPoolManager.getThreadPool().dispatch(createTask(tf,url, linkImagAddr, mc, post_id, p, conf));
                    } else {
                        p.updateStreamAttachment(post_id, attachment);
                    }
                } catch (MQException e) {
                    L.error("share link loop error(MQ)", e);
                    try {
                        mq.destroy();
                        mq.init();
                    } catch (MQException e1) {
                        L.error("MQ restart(destroy) error", e1);
                    }
                } catch (Exception e) {
                    L.error("share link loop error", e);
                }
            }
        } finally {
            MQCollection.destroyMQs();
            try {
                ThreadPoolManager.getThreadPool().stop();
            } catch (Exception e) {
            }
        }
    }

    private static Runnable createTask(final GenericTransceiverFactory tf, final String url, final String linkImagAddr, final XMemcached mc, final String post_id, final Platform p, final Configuration conf) {
        return new Runnable() {
            public void run() {
                RecordSet recs = new RecordSet();
                try {
                    Sharelink s = new Sharelink(tf, conf);
                    L.debug("Runnable=:" + url);
                    if (url.contains("z/v2mIRf") || url.contains("search?q=com.borqs.qiupu")) {
                        recs.add(returnSearch(url,p));
                    } else {
                        recs = s.shareLink(url, linkImagAddr);
                    }

                    if (recs.size()>0 && recs.getFirstRecord().getString("description").length() > 0) {
                        if (s.ifImgUrl(recs.getFirstRecord().getString("url"))) {
                            recs.getFirstRecord().put("description","");
                        }
                        p.updateStreamAttachment(post_id, recs.toString(false, false));
                        try {
                            mc.writeCache(url, recs.toString(false, false));
                        } catch (Exception e) {
                            L.debug("write mc error");
                        }
                    }
                    L.debug("Runnable end");
                } catch (Exception e) {
                }
            }
        };
    }

    private static Record returnSearch(String url, final Platform p) throws AvroRemoteException {
        Record out_rec = new Record();
        String ShortUrl = url.contains("z/v2mIRf")?url:p.generalShortUrl(url);
        out_rec.put("url", ShortUrl);
        out_rec.put("host", "api.borqs.com");
        out_rec.put("title", "Android B+");
        out_rec.put("description", "B+ 是由北京播思软件技术有限公司2011年重磅推出的一款集应用管理、应用商店、社交网络、数据备份等多功能于一体的Android客户端软件。提供了超大数量的本地Android中文应用软件，支持数以万计的资源搜索与直接下载，是您的手机应用最佳伴侣。稳定可靠的应用数据备份和恢复功能让您不必手机丢失或换手机而导致的数据丢失而烦恼。提供社交网络功能，随时随地分享您的应用和心得。".toString());
        out_rec.put("img_url", "http://apps.borqs.com/images/dashboard.jpg");
        out_rec.put("many_img_url", "http://apps.borqs.com/images/dashboard.jpg");
        return out_rec;
    }
}