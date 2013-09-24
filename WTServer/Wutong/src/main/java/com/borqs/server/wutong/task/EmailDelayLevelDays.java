package com.borqs.server.wutong.task;

import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SimpleConnectionFactory;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.email.EmailThreadPool;
import com.borqs.server.wutong.messagecenter.EmailDelayLevelAbstract;
import com.borqs.server.wutong.messagecenter.MessageCenter;
import com.borqs.server.wutong.messagecenter.MessageDelayCombineUtils;
import com.borqs.server.wutong.messagecenter.MessageUserDelayVisitTimeLogic;
import com.borqs.server.wutong.usersugg.SuggestedUserLogic;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EmailDelayLevelDays extends EmailDelayLevelAbstract {
    private static final Logger L = Logger.getLogger(EmailDelayLevelDays.class);
    private static final ConnectionFactory CONNECTION_FACTORY = new SimpleConnectionFactory();
    private static final String ALIYUN_ACCOUNT_DB = "mysql/borqsservice.mysql.rds.aliyuncs.com/accounts/accounts/accounts";
    //private static final String ALIYUN_ACCOUNT_DB = "mysql/localhost/accounts/root/1234";
    public static final int DAYS = 10;
    public static final String MAIL_PEOPLE_YOU_MAY_KNOW = "mail.people_you_may_know";
    public static final String TYPE_MESASGE_DELAY_VISIT_TIME = "1";

    public static ConnectionFactory getConnectionFactory() {
        return CONNECTION_FACTORY;
    }

    // 延迟邮件 1天
    //上线后需要将while(true)去掉，并在系统中设置好相应的定时运行时间
    public static void main(String[] args) throws IOException, InterruptedException {

        if ((args != null) && (args.length > 0)) {
            confPath = args[0];
        }
        GlobalConfig.loadFiles(confPath);
        GlobalLogics.init();

        Context ctx = new Context();

        //判断是否只延迟不合并
        delayLevel(ctx, MessageCenter.EMAIL_DELAY_TYPE_DAYS);

        peopleYouMayKnowDelayAndCombine(ctx);
        //处理系统推荐用户


        if (!EmailThreadPool.executor.isShutdown())
            EmailThreadPool.executor.shutdown();

    }

    /**
     * people you may know 单独与合并邮件的机制
     *
     * @param ctx
     */
    public static void peopleYouMayKnowDelayAndCombine(Context ctx) {
        // 扫描 user2 表，找出所有登录时间间隔超过10天的人
        String sql = "SELECT * FROM message_user_delaytime WHERE delay_time > 0 AND (CONCAT(UNIX_TIMESTAMP(),'000') - delay_time)/(1000*60*60*24) >" + DAYS;

        RecordSet recs = SQLExecutor.executeRecordSet(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sql, null);
        SuggestedUserLogic su = GlobalLogics.getSuggest();

        if (recs.size() < 1) {
            return;
        } else {
            String sql2 = "SELECT *  FROM user2 left join user_property on user_id = user and `key` = 30 WHERE user_id in(" + recs.joinColumnValues("user_id", ",") + ");";
            recs = SQLExecutor.executeRecordSet(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sql2, null);
        }


        for (Record r : recs) {
            // find people you may know
            RecordSet rs = su.getSuggestUserP(ctx, r.getString("user_id"), 7, false);
            Map<String, Record> map = rs.toRecordMap("user_id");

            String loginEmail1 = r.getString("login_email1");
            String loginEmail2 = r.getString("login_email2");
            String loginEmail3 = r.getString("login_email3");
            List<String> l = new ArrayList<String>();
            if (StringUtils.isNotBlank(loginEmail1))
                l.add(loginEmail1);
            if (StringUtils.isNotBlank(loginEmail2))
                l.add(loginEmail2);
            if (StringUtils.isNotBlank(loginEmail3))
                l.add(loginEmail3);

            //Send email
            for (String to : l)
                MessageDelayCombineUtils.combinePeopleYouMayKnow(ctx, rs, MAIL_PEOPLE_YOU_MAY_KNOW, r.getString("display_name"), to,StringUtils.defaultIfBlank(r.getString("value"),"en"));

        }

        try {
            for (Record r : recs) {
                MessageUserDelayVisitTimeLogic messageUserDelayVisitTimeLogic = GlobalLogics.getMessageUserDelayVisitTimeLogic();
                Record record = messageUserDelayVisitTimeLogic.getMessageByUserId(ctx, r.getString("user_id"), TYPE_MESASGE_DELAY_VISIT_TIME);
                if (record != null && record.size() > 0) {
                    record.put("delay_time", DateUtils.nowMillis());
                    messageUserDelayVisitTimeLogic.updateMessageUserDelayTimeById(ctx, record);
                } else {
                    Record r1 = Record.of("user_id", r.getString("user_id"), "type", TYPE_MESASGE_DELAY_VISIT_TIME, "delay_time", String.valueOf(DateUtils.nowMillis()));
                    messageUserDelayVisitTimeLogic.createUpdateMessageUserDelayVisitTime(ctx, r1);
                }
            }
        } catch (Exception e) {
            L.error(ctx, e, "update lastvisit time error!");
        }


    }


}