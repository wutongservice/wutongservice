package com.borqs.server.wutong.messagecenter;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

public interface MessageCenterLogic {
    boolean destroyMessageById(Context ctx,String message_id);
    boolean createMessageCenter(Context ctx, MessageCenter messageCenter);
    RecordSet getMessageByKey(Context ctx, String sendKey,String to,String content);
    RecordSet getMessageDistinctListByDelayType(Context ctx, String level);

    RecordSet getMessageDistinctListByDelayTypeAndSendKey(Context ctx, String delayType, String sendKey);

    RecordSet getMessageBySendKey(Context ctx, String sendKey);

    RecordSet getMessageByKeyAndTarget(Context ctx, String sendKey, String to, String target_id);

    Record getMessageFinish(Context ctx, String sendKey, String target_id, String to);
}