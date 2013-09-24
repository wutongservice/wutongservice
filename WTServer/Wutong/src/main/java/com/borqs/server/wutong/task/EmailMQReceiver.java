package com.borqs.server.wutong.task;

import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.mq.MQ;
import com.borqs.server.base.mq.MQCollection;
import com.borqs.server.base.util.ProcessUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.email.AsyncMailTask;
import com.borqs.server.wutong.email.EmailModel;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;

import java.io.File;
import java.io.IOException;

public class EmailMQReceiver {
    private static final Logger L = Logger.getLogger(EmailMQReceiver.class);

    public EmailMQReceiver() {
    }


    public static void main(String[] arguments) throws IOException {
        try {
            String confPath = "/home/zhengwei/workWT/dist-r3-distribution/etc/test.config.properties";
            //String confPath = "F:\\work\\refactProduct\\Dist\\src\\main\\etc\\test.config.properties";

            if ((arguments != null) && (arguments.length > 0)) {
                confPath = arguments[0];
            }
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
            int pid = ProcessUtils.writeProcessId(pidDirStr + "/email_mq_receiver.pid");
            while (true) {
                try {
                    String email = mq.receiveBlocked("email");
                    String json = StringUtils2.uncompress(email);
                    JsonNode jn = JsonUtils.parse(json);
                    String content = jn.path("content").getTextValue();
                    String sendEmailName = jn.path("sendEmailName").getTextValue();
                    String sendEmailPassword = jn.path("sendEmailPassword").getTextValue();
                    String title = jn.path("title").getTextValue();
                    String to = jn.path("to").getTextValue();
                    String username = jn.path("username").getTextValue();

                    EmailModel e = new EmailModel(GlobalConfig.get());
                    e.setContent(content);
                    e.setSendEmailName(sendEmailName);
                    e.setSendEmailPassword(sendEmailPassword);
                    e.setUsername(username);
                    e.setTitle(title);
                    e.setTo(to);


                    new AsyncMailTask().sendEmailFinal(new Context(), e);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        } finally {
            GlobalLogics.destroy();
            MQCollection.destroyMQs();
        }
    }
}