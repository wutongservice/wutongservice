package com.borqs.server.wutong.task;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.log.Logger;
import com.borqs.server.wutong.email.EmailThreadPool;
import com.borqs.server.wutong.messagecenter.EmailDelayLevelAbstract;
import com.borqs.server.wutong.messagecenter.MessageCenter;

import java.io.IOException;

public class EmailDelayLevelMinutes extends EmailDelayLevelAbstract {
    private static final Logger L = Logger.getLogger(EmailDelayLevelMinutes.class);

    // 延迟邮件 10分钟
    //上线后需要将while(true)去掉，并在系统中设置好相应的定时运行时间
    public static void main(String[] args) throws IOException, InterruptedException {
        if ((args != null) && (args.length > 0)) {
            confPath = args[0];
        }

        GlobalConfig.loadFiles(confPath);
        Configuration conf = GlobalConfig.get();
        Context ctx = new Context();
        delayLevel(ctx, MessageCenter.EMAIL_DELAY_TYPE_MINUTES);
        if (!EmailThreadPool.executor.isShutdown())
            EmailThreadPool.executor.shutdown();
    }


}