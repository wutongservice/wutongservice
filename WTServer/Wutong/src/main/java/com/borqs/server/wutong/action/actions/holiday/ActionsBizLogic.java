package com.borqs.server.wutong.action.actions.holiday;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import org.codehaus.jackson.JsonNode;

public interface ActionsBizLogic {
    void consumer(Context ctx, JsonNode jsonNode);
    void callBack(Context ctx,Record record);
}
