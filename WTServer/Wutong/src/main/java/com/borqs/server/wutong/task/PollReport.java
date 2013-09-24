package com.borqs.server.wutong.task;

import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SimpleConnectionFactory;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.group.GroupImpl;
import org.apache.commons.lang.StringUtils;

import java.util.Set;

public class PollReport {
    private static final ConnectionFactory CONNECTION_FACTORY = new SimpleConnectionFactory();
    private static final String ALIYUN_ACCOUNT_DB = "mysql/borqsservice.mysql.rds.aliyuncs.com/accounts/accounts/accounts";

    public static ConnectionFactory getConnectionFactory() {
        return CONNECTION_FACTORY;
    }

    public static void main(String[] args) throws Exception {
//        long time1 = DateUtils.getTimestamp(9);
//        long time2 = DateUtils.getTimestamp(18);
//
//        String sql0 = "select distinct user, created_time from poll_participants where poll_id=2841931693841516673 and created_time>"
//                + time1 + " order by created_time limit 5";
//
//        String sql1 = "select distinct user, created_time from poll_participants where poll_id=2841931693841516673 and created_time<"
//                + time2 + " order by created_time desc limit 5";
//
//        RecordSet recs = SQLExecutor.executeRecordSet(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sql0, null);
//        recs = SQLExecutor.executeRecordSet(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sql1, recs);
//        Record timeMap = new Record();
//        for (Record rec : recs) {
//            timeMap.put(rec.getString("user"), rec.getInt("created_time"));
//        }
//
//        String userIds = recs.joinColumnValues("user", ",");
//        if (StringUtils.isBlank(userIds)) {
//            System.out.println("No user vote.");
//        } else {
//            String sql = "select user_id, display_name from user2 where user_id in (" + userIds + ")";
//            recs = SQLExecutor.executeRecordSet(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sql, null);
//            Record nameMap = new Record();
//            for (Record rec : recs) {
//                nameMap.put(rec.getString("user_id"), rec.getString("display_name"));
//            }
//
//            Set<String> users = StringUtils2.splitSet(userIds, ",", true);
//            System.out.println("Name   |   vote time");
//            for (String userId : users) {
//                System.out.println(nameMap.getString(userId) + "   |   " + DateUtils.formatDateAndTimeCh(timeMap.getInt(userId)));
//            }
//        }

        GlobalConfig.loadFiles("/home/b516/BorqsServerPlatformWT/Dist/src/main/etc/test.config.properties");
        GroupImpl g = new GroupImpl();
        g.init();
        Context ctx = new Context();

        String[] arr = new String[] {
                "10001",                                                     //borqs id
                "#5",                                                        //local group
                "$10000000072", "#10000000072", "10000000072",               //public circle
                "$14000000001", "#14000000001", "14000000001",               //event
                "$20000000003", "#20000000003", "20000000003",               //page
                "changsheng.jiang@borqs.com", "*changsheng.jiang@borqs.com", //email
                "17810139250", "*17810139250", "13810139250", "*13810139250",//phone
                "2802955409634837694",                                       //virtual id
                "xvabi34dsy2_w=="                                            //unknown
        };

        for (String str : arr) {
            g.getTypeByStr(ctx, str);
        }
    }
}
