package com.borqs.server.base.data;


import org.apache.commons.lang.ArrayUtils;
import org.codehaus.jackson.JsonGenerator;

import java.io.IOException;
import java.util.Map;

public class AddonsBean {
    protected final Record addons = new Record();

    public AddonsBean() {
    }

    public Record getAddons() {
        return addons;
    }

    protected void writeAddonsJson(JsonGenerator jg, String[] cols) throws IOException {
        for (Map.Entry<String, Object> e : addons.entrySet()) {
            String col = e.getKey();
            if (ArrayUtils.contains(cols, col)) {
                jg.writeFieldName(col);
                jg.writeObject(e.getValue());
            }
        }
    }
}
