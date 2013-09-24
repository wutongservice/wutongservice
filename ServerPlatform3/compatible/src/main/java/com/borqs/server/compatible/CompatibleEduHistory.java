package com.borqs.server.compatible;


import com.borqs.server.platform.feature.account.EduHistory;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.CollectionUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CompatibleEduHistory {

    public static List<EduHistory> jsonToEduHistories(List<EduHistory> reuse, String json) {
        return jsonNodeToEduHistories(reuse, JsonHelper.parse(json));
    }

    public static List<EduHistory> jsonNodeToEduHistories(List<EduHistory> reuse, JsonNode jn) {
        if (reuse == null)
            reuse = new ArrayList<EduHistory>();

        for (int i = 0; i < jn.size(); i++)
            reuse.add(jsonNodeToEduHistory(jn.get(i)));

        return reuse;
    }

    public static EduHistory jsonToEduHistory(String json) {
        return jsonNodeToEduHistory(JsonHelper.parse(json));
    }

    public static EduHistory jsonNodeToEduHistory(JsonNode jn) {
        EduHistory eh = new EduHistory();
        deserializeEduHistory(jn, eh);
        return eh;
    }

    public static void deserializeEduHistory(JsonNode jn, EduHistory eh) {
        eh.deserialize(jn);
    }

    public static void serializeEduHistories(JsonGenerator jg, List<EduHistory> ehs) throws IOException {
        jg.writeStartArray();
        if (CollectionUtils.isNotEmpty(ehs)) {
            for (EduHistory eh : ehs) {
                if (eh != null)
                    serializeEduHistory(jg, eh);
            }
        }
        jg.writeEndArray();
    }

    public static void serializeEduHistory(JsonGenerator jg, EduHistory eh) throws IOException {
        eh.serialize(jg);
    }

}
