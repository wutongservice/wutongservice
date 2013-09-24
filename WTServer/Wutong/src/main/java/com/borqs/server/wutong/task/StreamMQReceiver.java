package com.borqs.server.wutong.task;

import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.mq.MQ;
import com.borqs.server.base.mq.MQCollection;
import com.borqs.server.base.util.ProcessUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.qiupu.QiupuLogics;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.stream.StreamLogic;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;

import java.io.File;
import java.io.IOException;

public class StreamMQReceiver {
    private static final Logger L = Logger.getLogger(StreamMQReceiver.class);

    public static void main(String[] args) throws IOException {
        try {
            String confPath = "/home/zhengwei/workWT/dist-r3-distribution/etc/test.config.properties";
            //String confPath = "F:\\work\\refactProduct\\Dist\\src\\main\\etc\\test.config.properties";
            if ((args != null) && (args.length > 0)) {
                confPath = args[0];
            }
//			Configuration conf = Configuration.loadFiles("Z:/workspace2/test/src/test/MQReceiver.properties").expandMacros();
            //Configuration conf = Configuration.loadFiles(confPath).expandMacros();
            GlobalConfig.loadFiles(confPath);
            QiupuLogics.init();
            MQCollection.initMQs();
            MQ mq = MQCollection.getMQ("platform");

            //pid
            String pidDirStr = FileUtils.getUserDirectoryPath() + "/.bpid";
            File pidDir = new File(pidDirStr);
            if (!pidDir.exists()) {
                FileUtils.forceMkdir(pidDir);
            }
            ProcessUtils.writeProcessId(pidDirStr + "/stream_mq_receiver.pid");

            GlobalLogics.init();
            QiupuLogics.init();
            StreamLogic stream = GlobalLogics.getStream();

            while (true) {
                String str = mq.receiveBlocked("stream");
                String json = str;
                boolean setFriend = false;
                JsonNode jn = null;
                try {
                    json = StringUtils2.uncompress(str);
                } catch (Exception e) {
                    json = str;
                }
                try {
                    jn = JsonUtils.parse(json);
                    L.info(null, "parse json " + jn);
                    setFriend = jn.path("setFriend").getBooleanValue();
                } catch (Exception e) {
                    L.debug(null, "parse json error " + e.getMessage());
                }
                if (setFriend) {
                    try {
                        String userId = jn.path("userId").getTextValue();
                        String friendIds = jn.path("friendIds").getTextValue();
                        int reason = jn.path("reason").getIntValue();
                        boolean can_comment = jn.path("can_comment").getBooleanValue();
                        boolean can_like = jn.path("can_like").getBooleanValue();
                        boolean can_reshare = jn.path("can_reshare").getBooleanValue();

                        //context cols
                        long viewerId = jn.path("viewerId").getLongValue();
                        String app = jn.path("app").getTextValue();
                        String ua = jn.path("ua").getTextValue();
                        String location = jn.path("location").getTextValue();
                        String language = jn.path("language").getTextValue();
                        int post_source = jn.path("post_source").getIntValue();
                        String scene = jn.path("scene").getTextValue();
                        Context ctx = new Context();
                        ctx.setViewerId(viewerId);
                        ctx.setAppId(app);
                        ctx.setUa(ua);
                        ctx.setLocation(location);
                        ctx.setLanguage(language);
                        ctx.putSession("scene",scene);


                        stream.sendPostBySetFriend0(ctx, userId, friendIds, reason, can_comment, can_like, can_reshare, post_source);
                    } catch (Exception e) {
                        L.debug(null, "Send set friend stream failed due to " + e.getMessage());
                        continue;
                    }
                } else {
                    try {
                        String userId = jn.path("userId").getTextValue();
                        int type = jn.path("type").getIntValue();
                        String msg = jn.path("msg").getTextValue();
                        String attachments = jn.path("attachments").getTextValue();
                        String appId = jn.path("appId").getTextValue();
                        String packageName = jn.path("packageName").getTextValue();
                        String apkId = jn.path("apkId").getTextValue();
                        String appData = jn.path("appData").getTextValue();
                        String mentions = jn.path("mentions").getTextValue();
                        boolean secretly = jn.path("secretly").getBooleanValue();
                        String cols = jn.path("cols").getTextValue();
                        String device = jn.path("device").getTextValue();
                        String location = jn.path("location").getTextValue();
                        boolean can_comment = jn.path("can_comment").getBooleanValue();
                        boolean can_like = jn.path("can_like").getBooleanValue();
                        boolean can_reshare = jn.path("can_reshare").getBooleanValue();
                        String add_to = jn.path("add_to").getTextValue();
                        int post_source = jn.path("post_source").getIntValue();
                        //context cols
                        long viewerId = jn.path("viewerId").getLongValue();
                        String app = jn.path("app").getTextValue();
                        String ua = jn.path("ua").getTextValue();
                        location = jn.path("location").getTextValue();
                        String language = jn.path("language").getTextValue();
                        String scene = jn.path("scene").getTextValue();

                        Context ctx = new Context();
                        ctx.setViewerId(viewerId);
                        ctx.setAppId(app);
                        ctx.setUa(ua);
                        ctx.setLocation(location);
                        ctx.setLanguage(language);
                        ctx.putSession("scene",scene);

                        //System.out.println("---------------------------finished!------------------------------");
                        stream.postP(ctx, userId, type, msg, attachments, appId, packageName, apkId, appData, mentions, secretly, cols, device, location, "", "", can_comment, can_like, can_reshare, add_to, post_source, Long.parseLong(scene));
                    } catch (Exception e) {
                        L.debug(null, "Send stream failed due to " + e.getMessage());
                        continue;
                    }
                }
            }
        } finally {
            MQCollection.destroyMQs();
        }
    }
}