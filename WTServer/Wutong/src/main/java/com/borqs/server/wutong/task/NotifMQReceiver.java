package com.borqs.server.wutong.task;

import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.mq.MQ;
import com.borqs.server.base.mq.MQCollection;
import com.borqs.server.base.util.ProcessUtils;
import com.borqs.server.base.util.ThreadUtils;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.base.util.threadpool.NotifTask;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.notif.*;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotifMQReceiver {
    private static final Logger L = Logger.getLogger(NotifMQReceiver.class);

    public static ExecutorService pool = Executors.newCachedThreadPool();

    public NotifMQReceiver() {
    }

    public NotificationSender getNotifSender(String nType) {
        if (StringUtils.equals(nType, Constants.NTF_ACCEPT_SUGGEST)) {
            return new AcceptSuggestNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_MY_APP_COMMENT)) {
            return new AppCommentNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_MY_APP_LIKE)) {
            return new AppLikeNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_NEW_FOLLOWER)) {
            return new NewFollowerNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_PROFILE_UPDATE)) {
            return new ProfileUpdateNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_APP_SHARE)) {
            return new SharedAppNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_OTHER_SHARE)) {
            return new SharedNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_PHOTO_SHARE)) {
            return new PhotoSharedNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_PHOTO_COMMENT)) {
            return new PhotoCommentNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_PHOTO_LIKE)) {
            return new PhotoLikeNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_FILE_SHARE)) {
            return new FileSharedNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_FILE_COMMENT)) {
            return new FileCommentNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_BORQS_APPLY)) {
            return new BorqsApplyNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_FILE_LIKE)) {
            return new FileLikeNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_MY_STREAM_COMMENT)) {
            return new StramCommentNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_MY_STREAM_LIKE)) {
            return new StreamLikeNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_MY_STREAM_RETWEET)) {
            return new StreamRetweetNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_PEOPLE_YOU_MAY_KNOW)) {
            return new PeopleYouMayKnowNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_REQUEST_ATTENTION)) {
            return new RequestAttentionNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_NEW_REQUEST)) {
            return new NewRequestNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_CREATE_ACCOUNT)) {
            return new CreateAccountNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_REPORT_ABUSE)) {
            return new ReportAbuseNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_GROUP_INVITE)) {
            return new GroupInviteNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_GROUP_APPLY)) {
            return new GroupApplyNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_POLL_INVITE)) {
            return new PollInviteNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_POLL_COMMENT)) {
            return new PollCommentNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_POLL_LIKE)) {
            return new PollLikeNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_GROUP_JOIN)) {
            return new GroupJoinNotifSender();
        } else if (StringUtils.equals(nType, Constants.NTF_SUBSCRIBE)) {
            return new SubscribeNotifSender();
        } else {
            return new SuggestUserNotifSender();
        }
    }

    public static void main(String[] arguments) throws IOException {
        try {
            String confPath = "/home/zhengwei/workWT/dist-r3-distribution/etc/test.config.properties";
            //String confPath = "F:\\work\\refactProduct\\Dist\\src\\main\\etc\\test.config.properties";
            if ((arguments != null) && (arguments.length > 0)) {
                confPath = arguments[0];
            }
//			Configuration conf = Configuration.loadFiles("/home/b516/BorqsServerPlatform2/test/src/test/MQReceiver.properties").expandMacros();
//            Configuration conf = Configuration.loadFiles(confPath).expandMacros();
            GlobalConfig.loadFiles(confPath);
            GlobalLogics.init();

            MQCollection.initMQs();
            MQ mq = MQCollection.getMQ("platform");

            //pid
            String pidDirStr = FileUtils.getUserDirectoryPath() + "/.bpid";
            File pidDir = new File(pidDirStr);
            if (!pidDir.exists()) {
                FileUtils.forceMkdir(pidDir);
            }
            int pid = ProcessUtils.writeProcessId(pidDirStr + "/notif_mq_receiver.pid");
//	        L.trace("pid: " + pid);

            int errorCount = 0;
            while (true) {
                try {
                    String json = mq.receiveBlocked("notif");
                    JsonNode jn = JsonUtils.parse(json);
                    String nType = jn.path("nType").getTextValue();
                    NotificationSender notif = new NotifMQReceiver().getNotifSender(nType);
//				L.trace("notif server: " + notif.NOTIF_SERVER_ADDR);

                    Object[][] args = new Object[12][];

                    ArrayNode an = (ArrayNode) jn.get("appId");
                    args[0] = new Object[an.size()];
                    for (int i = 0; i < an.size(); i++) {
                        args[0][i] = an.get(i).getValueAsText();
                    }

                    an = (ArrayNode) jn.get("senderId");
                    args[1] = new Object[an.size()];
                    for (int i = 0; i < an.size(); i++) {
                        args[1][i] = an.get(i).getValueAsText();
                    }

                    an = (ArrayNode) jn.get("title");
                    args[2] = new Object[an.size()];
                    for (int i = 0; i < an.size(); i++) {
                        args[2][i] = an.get(i).getValueAsText();
                    }

                    an = (ArrayNode) jn.get("action");
                    args[3] = new Object[an.size()];
                    for (int i = 0; i < an.size(); i++) {
                        args[3][i] = an.get(i).getValueAsText();
                    }

                    an = (ArrayNode) jn.get("type");
                    args[4] = new Object[an.size()];
                    for (int i = 0; i < an.size(); i++) {
                        args[4][i] = an.get(i).getValueAsText();
                    }

                    an = (ArrayNode) jn.get("uri");
                    args[5] = new Object[an.size()];
                    for (int i = 0; i < an.size(); i++) {
                        args[5][i] = an.get(i).getValueAsText();
                    }

                    an = (ArrayNode) jn.get("titleHtml");
                    args[6] = new Object[an.size()];
                    for (int i = 0; i < an.size(); i++) {
                        args[6][i] = an.get(i).getValueAsText();
                    }

                    an = (ArrayNode) jn.get("body");
                    args[7] = new Object[an.size()];
                    for (int i = 0; i < an.size(); i++) {
                        args[7][i] = an.get(i).getValueAsText();
                    }

                    an = (ArrayNode) jn.get("bodyHtml");
                    args[8] = new Object[an.size()];
                    for (int i = 0; i < an.size(); i++) {
                        args[8][i] = an.get(i).getValueAsText();
                    }

                    an = (ArrayNode) jn.get("objectId");
                    args[9] = new Object[an.size()];
                    for (int i = 0; i < an.size(); i++) {
                        args[9][i] = an.get(i).getValueAsText();
                    }

                    an = (ArrayNode) jn.get("scene");
                    args[10] = new Object[an.size()];
                    for (int i = 0; i < an.size(); i++) {
                        args[10][i] = an.get(i).getValueAsText();
                    }

                    an = (ArrayNode) jn.get("imageUrl");
                    args[11] = new Object[an.size()];
                    for (int i = 0; i < an.size(); i++) {
                        args[11][i] = an.get(i).getValueAsText();
                    }

                    an = (ArrayNode) jn.get("scope");
                    Object[] scopeArgs = new Object[an.size()];
                    for (int i = 0; i < an.size(); i++) {
                        scopeArgs[i] = an.get(i).getValueAsText();
                    }

                    //context cols
                    long viewerId = jn.path("viewerId").getLongValue();
                    String app = jn.path("app").getTextValue();
                    String ua = jn.path("ua").getTextValue();
                    String location = jn.path("location").getTextValue();
                    String language = jn.path("language").getTextValue();
                    String scene = jn.path("scene").getTextValue();

                    Context ctx = new Context();
                    ctx.setViewerId(viewerId);
                    ctx.setAppId(app);
                    ctx.setUa(ua);
                    ctx.setLocation(location);
                    ctx.setLanguage(language);
                    ctx.putSession("scene",scene);


//                    ThreadPoolManager.getThreadPool().dispatch(createTask(ctx, notif, scopeArgs, args, json));
                    pool.execute(createTask(ctx, notif, scopeArgs, args, json));
                    L.trace(ctx, "Send " + nType + " notification, json=" + json);
                    errorCount = 0;
                } catch (Throwable e) {
                    if (errorCount < 50) {
                        errorCount++;
                        L.error(null, e, "Send notif error");
                    } else {
                        ThreadUtils.sleep(1000);
                    }
                }
            }
        } finally {
            GlobalLogics.destroy();
            MQCollection.destroyMQs();
            pool.shutdown();
        }
    }

    private static Runnable createTask(final Context ctx, final NotificationSender notif, final Object[] scopeArgs, final Object[][] args, final String json) {
        return new NotifTask(json) {
            public void run() {
                notif.send(ctx, scopeArgs, args);
            }
        };
    }
}