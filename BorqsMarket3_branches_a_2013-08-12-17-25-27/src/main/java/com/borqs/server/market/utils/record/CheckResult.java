package com.borqs.server.market.utils.record;

import com.borqs.server.market.utils.record.Record;

import java.util.Map;

public class CheckResult extends Record {
    public CheckResult() {
    }

    public void addFieldError(String field, Object value, String errorMessage) {
        put(field, value);
        put(field + "_errorMessage", errorMessage);
    }

    public void addField(String field, Object value) {
        put(field, value);
        put(field + "_errorMessage", null);
    }

    public boolean ok() {
        for (Map.Entry<String, Object> entry : entrySet()) {
            if (entry.getValue() != null && entry.getKey().endsWith("_errorMessage"))
                return false;
        }
        return true;
    }

    public boolean error() {
        return !ok();
    }
}
