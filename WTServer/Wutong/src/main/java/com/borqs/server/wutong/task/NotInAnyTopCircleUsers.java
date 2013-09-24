package com.borqs.server.wutong.task;

import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SimpleConnectionFactory;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.group.GroupLogic;
import org.apache.commons.lang.StringUtils;

import java.util.HashSet;

public class NotInAnyTopCircleUsers {
    private static final ConnectionFactory CONNECTION_FACTORY = new SimpleConnectionFactory();
    private static final String ALIYUN_ACCOUNT_DB = "mysql/borqsservice.mysql.rds.aliyuncs.com/accounts/accounts/accounts";

    public static ConnectionFactory getConnectionFactory() {
        return CONNECTION_FACTORY;
    }

    public static void main(String[] args) throws Exception {
        Context ctx = new Context();
        String confPath = "/home/wutong/workWT/dist-r3-distribution/etc/production.config.properties";
        GlobalConfig.loadFiles(confPath);
        GlobalLogics.init();

        GroupLogic g = GlobalLogics.getGroup();
        HashSet<Long> set = new HashSet<Long>();
        String sql = "SELECT user_id FROM user2 where destroyed_time=0";
        RecordSet recs = SQLExecutor.executeRecordSet(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sql, null);
        for (Record rec : recs) {
            long userId = rec.checkGetInt("user_id");
            ctx.setViewerId(userId);
            String circleIds = g.getTopCircleIds(ctx);
            if (StringUtils.isBlank(circleIds))
                set.add(userId);
        }

        String userIds = StringUtils2.joinIgnoreBlank(",", set);
        System.out.println("Not in any top circle users: " + userIds);
    }
}
