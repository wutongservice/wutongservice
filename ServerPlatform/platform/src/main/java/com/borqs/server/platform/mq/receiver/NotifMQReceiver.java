package com.borqs.server.platform.mq.receiver;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.memcache.XMemcached;
import com.borqs.server.base.mq.MQ;
import com.borqs.server.base.mq.MQCollection;
import com.borqs.server.base.rpc.GenericTransceiverFactory;
import com.borqs.server.base.util.ProcessUtils;
import com.borqs.server.base.util.email.ThreadPoolManager;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.service.notification.*;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class NotifMQReceiver {
    private static final Logger L = LoggerFactory.getLogger(NotifMQReceiver.class);

    public NotifMQReceiver() {
    }

    public NotificationSender getNotifSender(Platform p, String nType) {
        if (StringUtils.equals(nType, Constants.NTF_ACCEPT_SUGGEST)) {
            return new AcceptSuggestNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_MY_APP_COMMENT)) {
            return new AppCommentNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_MY_APP_LIKE)) {
            return new AppLikeNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_NEW_FOLLOWER)) {
            return new NewFollowerNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_PROFILE_UPDATE)) {
            return new ProfileUpdateNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_APP_SHARE)) {
            return new SharedAppNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_OTHER_SHARE)) {
            return new SharedNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_PHOTO_SHARE)) {
            return new PhotoSharedNotifSender(p, null);
        }else if (StringUtils.equals(nType, Constants.NTF_PHOTO_COMMENT)) {
            return new PhotoCommentNotifSender(p, null);
        }else if (StringUtils.equals(nType, Constants.NTF_PHOTO_LIKE)) {
            return new PhotoLikeNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_FILE_SHARE)) {
            return new FileSharedNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_FILE_COMMENT)) {
            return new FileCommentNotifSender(p, null);
        }else if (StringUtils.equals(nType, Constants.NTF_BORQS_APPLY)) {
            return new BorqsApplyNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_FILE_LIKE)) {
            return new FileLikeNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_MY_STREAM_COMMENT)) {
            return new StramCommentNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_MY_STREAM_LIKE)) {
            return new StreamLikeNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_MY_STREAM_RETWEET)) {
            return new StreamRetweetNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_PEOPLE_YOU_MAY_KNOW)) {
            return new PeopleYouMayKnowNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_REQUEST_ATTENTION)) {
            return new RequestAttentionNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_NEW_REQUEST)) {
            return new NewRequestNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_CREATE_ACCOUNT)) {
            return new CreateAccountNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_REPORT_ABUSE)) {
            return new ReportAbuseNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_GROUP_INVITE)) {
            return new GroupInviteNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_GROUP_APPLY)) {
            return new GroupApplyNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_POLL_INVITE)) {
            return new PollInviteNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_POLL_COMMENT)) {
            return new PollCommentNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_POLL_LIKE)) {
            return new PollLikeNotifSender(p, null);
        } else if (StringUtils.equals(nType, Constants.NTF_GROUP_JOIN)) {
            return new GroupJoinNotifSender(p, null);
        } else {
            return new SuggestUserNotifSender(p, null);
        }
    }

    public static void main(String[] arguments) throws IOException {
        try {
            GenericTransceiverFactory tf = new GenericTransceiverFactory();
            String confPath = "/home/zhengwei/work2/dist/etc/test_web_server.properties";

            if ((arguments != null) && (arguments.length > 0)) {
                confPath = arguments[0];
            }
//			Configuration conf = Configuration.loadFiles("/home/b516/BorqsServerPlatform2/test/src/test/MQReceiver.properties").expandMacros();
            Configuration conf = Configuration.loadFiles(confPath).expandMacros();
            tf.setConfig(conf);
            tf.init();
            final Platform p = new Platform(tf);
            p.setConfig(conf);

            MQCollection.initMQs(conf);
            MQ mq = MQCollection.getMQ("platform");

            //pid
            String pidDirStr = FileUtils.getUserDirectoryPath() + "/.bpid";
            File pidDir = new File(pidDirStr);
            if (!pidDir.exists()) {
                FileUtils.forceMkdir(pidDir);
            }
            int pid = ProcessUtils.writeProcessId(pidDirStr + "/notif_mq_receiver.pid");
//	        L.trace("pid: " + pid);

            while (true) {
                String json = mq.receiveBlocked("notif");
                JsonNode jn = JsonUtils.parse(json);
                String nType = jn.path("nType").getTextValue();
                NotificationSender notif = new NotifMQReceiver().getNotifSender(p, nType);
//				L.trace("notif server: " + notif.NOTIF_SERVER_ADDR);

                Object[][] args = new Object[10][];

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

                an = (ArrayNode) jn.get("scope");
                Object[] scopeArgs = new Object[an.size()];
                for (int i = 0; i < an.size(); i++) {
                    scopeArgs[i] = an.get(i).getValueAsText();
                }

                try {
                    ThreadPoolManager.getThreadPool().dispatch(createTask(notif, scopeArgs, args));
                } catch (Exception e) {
                    L.debug("Send " + nType + " notification error due to " + e.getMessage());
                    continue;
                }
            }
        } finally {
            MQCollection.destroyMQs();
        }
    }

    private static Runnable createTask(final NotificationSender notif, final Object[] scopeArgs, final Object[][] args) {
        return new Runnable() {
            public void run() {
                try {
                    notif.send(scopeArgs, args);
                } catch (AvroRemoteException e) {
                    L.debug("Send notification error due to " + e.getMessage());
                }
            }
        };
    }
}