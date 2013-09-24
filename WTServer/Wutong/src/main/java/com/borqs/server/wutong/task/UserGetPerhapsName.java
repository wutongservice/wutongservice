package com.borqs.server.wutong.task;

import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.mq.MQCollection;
import com.borqs.server.wutong.account2.AccountImpl;

import java.io.IOException;

public class UserGetPerhapsName {
    private static final Logger L = Logger.getLogger(UserGetPerhapsName.class);

    public static void main(String[] args) throws IOException {
        try {

            Context ctx = new Context();
			String confPath = "/home/zhengwei/workWT/dist-r3-distribution/etc/test.config.properties";
//            String confPath = "file://D:\\2workspace\\BorqsServerPlatform2\\mytest\\src\\main\\java\\company\\test\\PlatformWebServerTest.properties";

            if ((args != null) && (args.length > 0)) {
                confPath = args[0];
            }
//			Configuration conf = Configuration.loadFiles("/home/b516/BorqsServerPlatform2/test/src/test/MQReceiver.properties").expandMacros();
//            Configuration conf = Configuration.loadFiles(confPath).expandMacros();
            GlobalConfig.loadFiles(confPath);

            AccountImpl account = new AccountImpl();
            account.init();
            RecordSet recs = account.findAllUserIds(ctx, true);

            for (Record rec : recs) {
                String user_id = rec.getString("user_id");
                String perhapsName = account.getPerhapsNameP(ctx, account.formatUrlP(ctx, user_id));
//                System.out.println(user_id + "=" + perhapsName);
                Record r = new Record();
                r.put("perhaps_name",perhapsName);

                account.updateAccount(ctx, user_id,r,"");
            }
        } finally {
            MQCollection.destroyMQs();
        }
    }


}