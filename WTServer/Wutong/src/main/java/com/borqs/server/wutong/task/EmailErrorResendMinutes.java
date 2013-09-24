package com.borqs.server.wutong.task;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.email.AsyncMailTask;
import com.borqs.server.wutong.email.EmailModel;
import com.borqs.server.wutong.email.EmailThreadPool;
import com.borqs.server.wutong.messagecenter.EmailDelayLevelAbstract;
import com.borqs.server.wutong.messagecenter.MessageCenterLogic;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;

public class EmailErrorResendMinutes extends EmailDelayLevelAbstract {
    private static final Logger L = Logger.getLogger(EmailErrorResendMinutes.class);

    // 延迟邮件 10分钟
    //上线后需要将while(true)去掉，并在系统中设置好相应的定时运行时间
    public static void main(String[] args) throws IOException, InterruptedException {
        if ((args != null) && (args.length > 0)) {
            confPath = args[0];
        }

        GlobalConfig.loadFiles(confPath);
        GlobalLogics.init();
        Configuration conf = GlobalConfig.get();
        Context ctx = new Context();
        sendErrorEmail(ctx);
        if (!EmailThreadPool.executor.isShutdown())
            EmailThreadPool.executor.shutdown();
    }

    /**
     * 处理重发错误邮件
     */
    private static void sendErrorEmail(Context ctx) {
        MessageCenterLogic messageCenterLogic = GlobalLogics.getMessageCenter();
        RecordSet rs = messageCenterLogic.getMessageBySendKey(ctx, "default");
        EmailModel emailModule = EmailModel.getDefaultEmailModule(GlobalConfig.get());

        for (Record r : rs) {
            emailModule.setContent(r.getString("content"));
            emailModule.setTo(r.getString("to_"));
            emailModule.setUsername(r.getString("username"));
            emailModule.setTitle(r.getString("title"));

            new AsyncMailTask().sendEmailFinal(ctx, emailModule);
        }
        if (rs.size() > 0) {
            String ids = StringUtils.join(rs.getStringColumnValues("message_id"), ",");
            messageCenterLogic.destroyMessageById(ctx, ids);
        }
    }

}