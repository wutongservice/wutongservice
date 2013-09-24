package com.borqs.server.wutong.account2;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.messagecenter.MessageUserDelayVisitTimeLogic;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class UserLastVisitTimeCache {
    public static ConcurrentMap<String, Long> map = new ConcurrentHashMap<String, Long>();
    public static final String TYPE_MESASGE_DELAY_VISIT_TIME = "1";
    private static final Logger L = Logger.getLogger(UserLastVisitTimeCache.class);

    public static void set(String userid, long time) {

        Long l = map.get(userid);
        long now = DateUtils.nowMillis();
        //TODO 分隔时间 5 分钟
        if (l != null && l > 0) {

            long minutes = (now - l) / (1000 * 60);
            if (minutes > 5) {
                map.put(userid, time);
                //save
                updateVisitTime(userid, now);
            }
        } else {
            map.put(userid, time);
            //save
            updateVisitTime(userid, now);
        }

    }


    private static void updateVisitTime(String userId, long time) {
        try {
            Context ctx = new Context();
            AccountLogic account = GlobalLogics.getAccount();
            account.updateVisitTime(ctx, userId, time);

            MessageUserDelayVisitTimeLogic messageUserDelayVisitTimeLogic = GlobalLogics.getMessageUserDelayVisitTimeLogic();
            Record record = messageUserDelayVisitTimeLogic.getMessageByUserId(ctx, userId, TYPE_MESASGE_DELAY_VISIT_TIME);
            if (record != null && record.size() > 0) {
                record.put("delay_time", DateUtils.nowMillis());
                messageUserDelayVisitTimeLogic.updateMessageUserDelayTimeById(ctx, record);
            } else {
                Record r1 = Record.of("user_id", userId, "type", TYPE_MESASGE_DELAY_VISIT_TIME, "delay_time", String.valueOf(DateUtils.nowMillis()));
                messageUserDelayVisitTimeLogic.createUpdateMessageUserDelayVisitTime(ctx, r1);
            }
        } catch (Exception e) {
            L.error(new Context(),e,"save visit time error!");
        }
    }
}