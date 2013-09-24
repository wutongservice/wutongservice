package com.borqs.server.platform.data;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class Schemas {
    public static void standardize(Schema schema, Map<String, Object> rec) {
        Validate.notNull(schema);
        if (rec == null)
            return;

        for (String k : rec.keySet()) {
            int type = schema.getType(k);
            Object v = Values.to(rec.get(k), type);
            rec.put(k, v);
        }
    }

    public static void standardize(Schema schema, ObjectNode rec) {
        Validate.notNull(schema);
        if (rec == null)
            return;

        Iterator<String> fieldNamesIter = rec.getFieldNames();
        while (fieldNamesIter.hasNext()) {
            String fieldName = fieldNamesIter.next();
            int type = schema.getType(fieldName);

            Object v = Values.to(rec.get(fieldName), type);
            if (v == null) {
                rec.put(fieldName, (JsonNode) null);
            } else if (v instanceof Boolean) {
                rec.put(fieldName, (Boolean) v);
            } else if (v instanceof Long) {
                rec.put(fieldName, (Long) v);
            } else if (v instanceof Double) {
                rec.put(fieldName, (Double) v);
            } else if (v instanceof String) {
                rec.put(fieldName, (String) v);
            } else if (v instanceof JsonNode) {
                rec.put(fieldName, (JsonNode) v);
            } else {
                throw new ServerException(E.DATA, "json node (record) standardize error");
            }

        }
    }

    public static void standardize(Schema schema, Collection<Record> recs) {
        for (Record rec : recs)
            standardize(schema, rec);
    }


    public static void standardize(Schema schema, ArrayNode recs) {
        for (int i = 0; i < recs.size(); i++) {
            JsonNode jn = recs.get(i);
            if (!(jn instanceof ObjectNode))
                throw new ServerException(E.DATA, "json node (array) standardize error");

            standardize(schema, (ObjectNode) jn);
        }
    }

    public static void standardize(Schema schema, JsonNode jn) {
        if (jn.isObject()) {
            standardize(schema, (ObjectNode) jn);
        } else if (jn.isArray()) {
            standardize(schema, (ArrayNode) jn);
        }
    }

    public static void checkRecordIncludeColumns(Record rec, String... cols) {
        for (String col : cols) {
            if (!rec.has(col))
                throw new ServerException(E.DATA, "Must include column '%s'", col);
        }
    }

    public static void checkRecordIncludeColumns(ObjectNode rec, String... cols) {
        for (String col : cols) {
            if (!rec.has(col))
                throw new ServerException(E.DATA, "Must include column '%s'", col);
        }
    }

    public static void checkRecordExcludeColumns(Record rec, String... cols) {
        for (String col : cols) {
            if (rec.has(col))
                throw new ServerException(E.DATA, "Can't include column '%s'", col);
        }
    }

    public static void checkRecordExcludeColumns(ObjectNode rec, String... cols) {
        for (String col : cols) {
            if (rec.has(col))
                throw new ServerException(E.DATA, "Can't include column '%s'", col);
        }
    }

    public static void checkRecordColumnsIn(Record rec, String... cols) {
        for (String col : rec.getColumnsArray()) {
            if (!ArrayUtils.contains(cols, col))
                throw new ServerException(E.DATA, "Unknown column '%s'", col);
        }
    }

    public static void checkRecordColumnsIn(ObjectNode rec, String... cols) {
        Iterator<String> fieldNamesIter = rec.getFieldNames();
        while (fieldNamesIter.hasNext()) {
            String fieldName = fieldNamesIter.next();
            if (!ArrayUtils.contains(cols, fieldName))
                throw new ServerException(E.DATA, "Unknown column '%s'", fieldName);
        }
    }

    public static void checkSchemaIncludeColumns(Schema schema, String... cols) {
        for (String col : cols) {
            if (!schema.has(col))
                throw new ServerException(E.DATA, "Unknown column '%s'", col);
        }
    }

    public static void checkSchemaIncludeGroupColumns(Schema schema, String group, String... cols) {
        for (String col : cols) {
            if (!schema.hasColumnInGroup(col, group))
                throw new ServerException(E.DATA, "Unknown column '%s'", col);
        }
    }
}
