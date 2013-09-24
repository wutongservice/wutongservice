package com.borqs.server.wutong.conversation;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

import java.util.List;
import java.util.Map;

public interface ConversationLogic {
    boolean createConversation(Context ctx,Record conversation) ;

    boolean deleteConversation(Context ctx,int target_type, String target_id, int reason, long from) ;

    RecordSet getConversation(Context ctx,int target_type, String target_id, List<String> reasons, long from, int page, int count) ;

    boolean ifExistConversation(Context ctx,int target_type, String target_id, int reason, long from) ;

    boolean updateConversationTarget(Context ctx,String old_target_id, String new_target_id) ;


    boolean createConversationP(Context ctx, int target_type, String target_id, int reason, String fromUsers) ;

    boolean deleteConversationP(Context ctx, int target_type, String target_id, int reason, long from) ;

    boolean enableConversion(Context ctx, int targetType, String targetId, int enabled);

    int getEnabled(Context ctx, int targetType, String targetId);

    Map<String, Integer> getEnabledByTargetIds(Context ctx, String targetIds);
}