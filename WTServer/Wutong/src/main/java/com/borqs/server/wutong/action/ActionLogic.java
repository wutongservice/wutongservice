package com.borqs.server.wutong.action;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

public interface ActionLogic {
    RecordSet getActionConfig(Context ctx, String target_id, String name);
    RecordSet getAllActionConfigs(Context ctx);

    /**
     * check the post object
     * type name postId add to queue
     *
     * @param ctx
     * @param post
     */
    void sendActionQueue(Context ctx, Record post);

}