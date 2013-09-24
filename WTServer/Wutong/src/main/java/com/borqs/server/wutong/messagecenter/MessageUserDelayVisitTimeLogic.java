package com.borqs.server.wutong.messagecenter;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;

public interface MessageUserDelayVisitTimeLogic {
    boolean createUpdateMessageUserDelayVisitTime(Context ctx, Record messageUserDelay);

    Record getMessageByUserId(Context ctx, String userId, String type);

    boolean updateMessageUserDelayTimeById(Context ctx, Record r);
}