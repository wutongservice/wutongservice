package com.borqs.server.platform.data;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.io.IOHelper;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.Copyable;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializableWithType;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.util.*;


public class Record extends LinkedHashMap<String, Object> implements Copyable<Record>, JsonSerializableWithType {
    public Record() {
    }

    public Record(Map<String, ? extends Object> values) {
        super(values);
    }

    public static boolean isEmpty(Record rec) {
        return rec == null || rec.isEmpty();
    }

    public boolean has(String col) {
        return containsKey(col);
    }

    public boolean hasAll(String... cols) {
        return hasAll(Arrays.asList(cols));
    }

    public boolean hasAll(Collection<String> cols) {
        if (CollectionUtils.isEmpty(cols))
            return true;

        for (String col : cols) {
            if (!has(col))
                return false;
        }
        return true;
    }

    public boolean hasOne(String... cols) {
        return hasOne(Arrays.asList(cols));
    }

    public boolean hasOne(Collection<String> cols) {
        if (CollectionUtils.isEmpty(cols))
            return true;

        for (String col : cols) {
            if (has(col))
                return true;
        }
        return false;
    }


    public String[] getColumnsArray() {
        Set<String> cols = keySet();
        return cols.toArray(new String[cols.size()]);
    }

    public Object getScalar() {
        return isEmpty() ? null : values().iterator().next();
    }

    public void putMissing(String col, Object o) {
        if (!has(col))
            put(col, o);
    }

    public void putIf(String col, Object o, boolean b) {
        if (b)
            put(col, o);
    }


    public byte[] toBytes() {
        return IOHelper.toBytes(this);
    }

    public static Record fromBytes(byte[] bytes, int off, int len) {
        return (Record) IOHelper.fromBytes(bytes, off, len);
    }

    public static Record fromBytes(byte[] bytes) {
        return (Record) IOHelper.fromBytes(bytes);
    }

    @Override
    public void serializeWithType(JsonGenerator jsonGenerator, SerializerProvider serializerProvider, TypeSerializer typeSerializer) throws IOException, JsonProcessingException {
        jsonWrite(jsonGenerator, false);
    }

    @Override
    public void serialize(JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        jsonWrite(jsonGenerator, false);
    }

    public void jsonWrite(JsonGenerator jg, boolean ignoreNull) throws IOException {
        jg.writeStartObject();
        for (Map.Entry<String, Object> e : entrySet()) {
            String col = e.getKey();
            Object val = e.getValue();
            if (ignoreNull) {
                if (val != null) {
                    jg.writeFieldName(col);
                    JsonHelper.writeValue(jg, val, null);
                }
            } else {
                jg.writeFieldName(col);
                JsonHelper.writeValue(jg, val, null);
            }
        }
        jg.writeEndObject();
    }

    @Override
    public String toString() {
        return toJson();
    }

    public String toJson() {
        return toJson(false, true);
    }

    public String toJson(final boolean ignoreNull, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                jsonWrite(jg, ignoreNull);
            }
        }, human);
    }


    public JsonNode toJsonNode() {
        return JsonHelper.parse(toJson(false, false));
    }


    public static Record fromJson(String json) {
        return JsonHelper.fromJson(json, Record.class);
    }

    public static Record fromJsonNode(JsonNode jn) {
        return JsonHelper.fromJsonNode(jn, Record.class);
    }

    public String getString(String col, String def) {
        try {
            Object v = get(col);
            return v != null ? Values.toString(v) : def;
        } catch (Exception e) {
            return def;
        }
    }

    public boolean getBoolean(String col, boolean def) {
        try {
            Object v = get(col);
            return v != null ? Values.toBoolean(v) : def;
        } catch (Exception e) {
            return def;
        }
    }

    public long getInt(String col, long def) {
        try {
            Object v = get(col);
            return v != null ? Values.toInt(v) : def;
        } catch (Exception e) {
            return def;
        }
    }

    public double getFloat(String col, double def) {
        try {
            Object v = get(col);
            return v != null ? Values.toFloat(v) : def;
        } catch (Exception e) {
            return def;
        }
    }

    public JsonNode getJson(String col, JsonNode def) {
        try {
            return JsonHelper.parse(getString(col));
        } catch (Exception e) {
            return def;
        }
    }

    public String getString(String col) {
        return getString(col, "");
    }

    public long getInt(String col) {
        return getInt(col, 0L);
    }

    public double getFloat(String col) {
        return getFloat(col, 0.0);
    }

    private void checkColumn(String col) {
        if (!has(col))
            throw new ServerException(E.DATA, "Missing column '%s'", col);
    }

    public Object checkGet(String col) {
        checkColumn(col);
        return get(col);
    }

    public String checkGetString(String col) {
        checkColumn(col);
        return getString(col);
    }

    public long checkGetInt(String col) {
        checkColumn(col);
        return getInt(col);
    }

    public boolean checkGetBoolean(String col) {
        checkColumn(col);
        return getBoolean(col, false);
    }

    public double checkGetFloat(String col) {
        checkColumn(col);
        return getFloat(col);
    }

    public Record renameColumn(String oldCol, String newCol) {
        if (!StringUtils.equals(oldCol, newCol) && has(oldCol)) {
            Object o = get(oldCol);
            remove(oldCol);
            put(newCol, o);
        }
        return this;
    }

    @Override
    public Record copy() {
        Record rec = new Record();
        rec.putAll(this);
        return rec;
    }

    public Record set(String col, Object v) {
        put(col, v);
        return this;
    }


    public Record setMissing(String col, Object v) {
        putMissing(col, v);
        return this;
    }

    public Record setIf(String col, Object v, boolean b) {
        if (b)
            put(col, v);
        return this;
    }

    public Record replace(String col, Object v) {
        if (has(col))
            put(col, v);
        return this;
    }


    public void copyTo(Record rec) {
        if (rec != null)
            rec.putAll(this);
    }

    public Record removeColumns(Collection<String> cols) {
        for (String col : cols)
            remove(col);
        return this;
    }

    public Record removeColumns(String... cols) {
        return removeColumns(Arrays.asList(cols));
    }

    public Record retainColumns(Collection<String> cols) {
        ArrayList<String> allCols = new ArrayList<String>(keySet());
        for (String col : allCols) {
            if (!cols.contains(col))
                remove(col);
        }
        return this;
    }

    public Record retainColumns(String... cols) {
        return retainColumns(Arrays.asList(cols));
    }

    public String findColumnByValueIn(Object value, String[] scopeCols) {
        if (scopeCols == null)
            scopeCols = getColumnsArray();

        for (String col : scopeCols) {
            if (col != null) {
                if (ObjectUtils.equals(value, get(col)))
                    return col;
            }
        }
        return null;
    }



    public static Record of(String c1, Object v1) {
        Record rec = new Record();
        rec.put(c1, v1);
        return rec;
    }

    public static Record of(String c1, Object v1, String c2, Object v2) {
        Record rec = new Record();
        rec.put(c1, v1);
        rec.put(c2, v2);
        return rec;
    }

    public static Record of(String c1, Object v1, String c2, Object v2, String c3, Object v3) {
        Record rec = new Record();
        rec.put(c1, v1);
        rec.put(c2, v2);
        rec.put(c3, v3);
        return rec;
    }

    public static Record of(Object[][] values) {
        Record rec = new Record();
        rec.putAll(CollectionsHelper.arraysToMap(values));
        return rec;
    }
}
