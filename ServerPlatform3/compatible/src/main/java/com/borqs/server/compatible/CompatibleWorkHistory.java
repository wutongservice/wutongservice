package com.borqs.server.compatible;


import com.borqs.server.platform.feature.account.WorkHistory;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.CollectionUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CompatibleWorkHistory {

    public static List<WorkHistory> jsonToWorkHistories(List<WorkHistory> reuse, String json) {
        return jsonNodeToWorkHistories(reuse, JsonHelper.parse(json));
    }

    public static List<WorkHistory> jsonNodeToWorkHistories(List<WorkHistory> reuse, JsonNode jn) {
        if (reuse == null)
            reuse = new ArrayList<WorkHistory>();
        for (int i = 0; i < jn.size(); i++)
            reuse.add(jsonNodeToWorkHistory(jn.get(i)));
        return reuse;
    }

    public static WorkHistory jsonToWorkHistory(String json) {
        return jsonNodeToWorkHistory(JsonHelper.parse(json));
    }

    public static WorkHistory jsonNodeToWorkHistory(JsonNode jn) {
        WorkHistory wh = new WorkHistory();
        deserializeWorkHistory(jn, wh);
        return wh;
    }

    public static void deserializeWorkHistory(JsonNode jn, WorkHistory wh) {
        wh.deserialize(jn);
    }

    public static void serializeWorkHistories(JsonGenerator jg, List<WorkHistory> whs) throws IOException {
        jg.writeStartArray();
        if (CollectionUtils.isNotEmpty(whs)) {
            for (WorkHistory wh : whs) {
                if (wh != null)
                    serializeWorkHistory(jg, wh);
            }
        }
        jg.writeEndArray();
    }

    public static void serializeWorkHistory(JsonGenerator jg, WorkHistory wh) throws IOException {
        wh.serialize(jg);
    }

}
