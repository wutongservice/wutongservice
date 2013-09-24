package com.borqs.server.market.utils.record;


import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.PrimitiveTypeConverter;
import org.codehaus.jackson.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

public class Record extends LinkedHashMap<String, Object> {
    public Record() {
    }

    public Record(Map<? extends String, ?> m) {
        super(m);
    }

    public static Record of(Object... kvs) {
        Record rec = new Record();
        CC.map(rec, kvs);
        return rec;
    }

    public boolean hasField(String field) {
        return containsKey(field);
    }

    public void removeField(String field) {
        remove(field);
    }

    public void removeFields(String... fields) {
        for (String field : fields)
            remove(field);
    }

    public Record set(String field, Object val) {
        put(field, val);
        return this;
    }

    public Record sets(Map<String, Object> m) {
        putAll(m);
        return this;
    }

    public Record sets(Object... kvs) {
        return sets(CC.map(kvs));
    }

//    public boolean getBoolean(String field, boolean def) {
//        return containsKey(field) ? (Boolean) get(field) : def;
//    }
//
//    public int getInt(String field, int def) {
//        return containsKey(field) ? (Integer) get(field) : def;
//    }
//
//    public int getInt(String field) {
//        return getInt(field, 0);
//    }
//
//    public String getString(String field, String def) {
//        return containsKey(field) ? (String) get(field) : def;
//    }
//
//    public String getString(String field) {
//        return getString(field, null);
//    }
//

    public JsonNode getJsonNode(String field, JsonNode def) {
        return hasField(field) ? (JsonNode) get(field) : def;
    }

    public JsonNode getJsonNode(String field) {
        return getJsonNode(field, null);
    }


    public boolean asBoolean(String field, boolean def) {
        return hasField(field) ? PrimitiveTypeConverter.toBoolean(get(field), def) : def;
    }

    public int asInt(String field, int def) {
        return hasField(field) ? PrimitiveTypeConverter.toInt(get(field), def) : def;
    }

    public int asInt(String field) {
        return asInt(field, 0);
    }

    public long asLong(String field, long def) {
        return hasField(field) ? PrimitiveTypeConverter.toLong(get(field), def) : def;
    }

    public long asLong(String field) {
        return asLong(field, 0L);
    }

    public String asString(String field, String def) {
        return hasField(field) ? PrimitiveTypeConverter.toStr(get(field)) : def;
    }

    public String asString(String field) {
        return asString(field, null);
    }
}
