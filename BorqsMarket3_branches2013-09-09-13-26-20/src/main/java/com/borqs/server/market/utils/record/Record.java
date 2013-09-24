package com.borqs.server.market.utils.record;


import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.JsonUtils;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.PrimitiveTypeConverter;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.type.Alias;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.codehaus.jackson.JsonNode;

import java.util.*;

@Alias("record")
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

    public String[] getFieldsAsArray() {
        Set<String> fields = getFields();
        return fields.toArray(new String[fields.size()]);
    }

    public Set<String> getFields() {
        return keySet();
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

    public Boolean asBooleanWithNull(String field) {
        return hasField(field) ? PrimitiveTypeConverter.toBoolean(get(field)) : null;
    }

    public int asInt(String field, int def) {
        return hasField(field) ? PrimitiveTypeConverter.toInt(get(field), def) : def;
    }

    public Integer asIntObject(String field) {
        return hasField(field) ? PrimitiveTypeConverter.toInt(get(field)) : null;
    }

    public int asInt(String field) {
        return asInt(field, 0);
    }

    public Long asLongObject(String field) {
        return hasField(field) ? PrimitiveTypeConverter.toLong(get(field)) : null;
    }

    public long asLong(String field, long def) {
        return hasField(field) ? PrimitiveTypeConverter.toLong(get(field), def) : def;
    }

    public long asLong(String field) {
        return asLong(field, 0L);
    }

    public Double asDoubleObject(String field) {
        return hasField(field) ? PrimitiveTypeConverter.toDouble(get(field)) : null;
    }

    public double asDouble(String field, double def) {
        return hasField(field) ? PrimitiveTypeConverter.toDouble(get(field), def) : def;
    }

    public double asDouble(String field) {
        return asDouble(field, 0.0);
    }

    public String asString(String field, String def) {
        if (hasField(field)) {
            Object val = get(field);
            if (val instanceof JsonNode) {
                return JsonUtils.toJson(val, false);
            } else {
                return PrimitiveTypeConverter.toStr(val);
            }
        } else {
            return def;
        }
    }

    public String asJoinedString(String field, String sep) {
        if (hasField(field)) {
            Object val = get(field);
            if (val == null) {
                return null;
            } else {
                if (val.getClass().isArray()) {
                    return StringUtils.join((Object[]) val, sep);
                } else if (val instanceof Collection) {
                    return StringUtils.join((Collection) val, sep);
                } else {
                    return val.toString();
                }
            }
        } else {
            return null;
        }
    }

    public String asString(String field) {
        return asString(field, null);
    }

    public FileItem asFileItem(String field) {
        Object val = get(field);
        return val instanceof FileItem && !((FileItem) val).isFormField() ? (FileItem) val : null;
    }

    @SuppressWarnings("unchecked")
    public boolean isType(String field, Class type) {
        if (hasField(field)) {
            Object val = get(field);
            return val != null && type.isAssignableFrom(val.getClass());
        } else {
            return false;
        }
    }

    public Record renameField(String oldField, String newField) {
        if (newField != null && !StringUtils.equals(oldField, newField) && hasField(oldField)) {
            Object val = remove(oldField);
            put(newField, val);
        }
        return this;
    }

    public Record renameFields(Map<String, String> oldFieldsAndNewFields) {
        if (MapUtils.isNotEmpty(oldFieldsAndNewFields)) {
            for (Map.Entry<String, String> e : oldFieldsAndNewFields.entrySet())
                renameField(e.getKey(), e.getValue());
        }
        return this;
    }

    public Record renameFields(String... oldFieldsAndNewFields) {
        return renameFields(CC.strMap(oldFieldsAndNewFields));
    }

    public Record retainsFields(String... fields) {
        if (ArrayUtils.isEmpty(fields)) {
            clear();
        } else {
            HashSet<String> keys = new HashSet<String>(keySet());
            for (String key : keys) {
                if (!ArrayUtils.contains(fields, key))
                    remove(key);
            }
        }
        return this;
    }

    public boolean transactValue(String field, ValueTransactor transactor) {
        if (!hasField(field) || transactor == null)
            return false;

        Object val = get(field);
        val = transactor.transact(field, val);
        put(field, val);
        return true;
    }

    public static interface ValueTransactor {
        Object transact(String field, Object val);
    }


    public Record aggregateMultipleLocale(String name) {
        Params.aggregateMultipleLocale(this, name);
        return this;
    }

    public Record aggregateMultipleLocale(String... names) {
        for (String name : names) {
            aggregateMultipleLocale(name);
        }
        return this;
    }

    public Record disperseMultipleLocale(String name) {
        Params.disperseMultipleLocale(this, name);
        return this;
    }

    public Record disperseMultipleLocale(String... names) {
        for (String name : names) {
            disperseMultipleLocale(name);
        }
        return this;
    }
}
