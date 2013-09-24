package com.borqs.server.platform.data;


import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.lang.ObjectUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class ValuesNewAccount {
    public static final int NONE = 0;
    public static final int BOOLEAN = 1;
    public static final int INT = 2;
    public static final int FLOAT = 3;
    public static final int STRING = 4;
    public static final int JSON = 5;

    public static Object to(Object o, int type) {
        if (o == null)
            return null;

        switch (type) {
            case BOOLEAN: {
                if (o instanceof Boolean)
                    return o;
                else if (o instanceof Number)
                    return ((Number) o).longValue() != 0L;
                else if (o instanceof CharSequence || o instanceof Character)
                    return Boolean.parseBoolean(o.toString());
                else
                    throw new IllegalArgumentException();
            }

            case INT: {
                if (o instanceof Boolean)
                    return ((Boolean) o) ? 1L : 0L;
                else if (o instanceof Number)
                    return ((Number) o).longValue();
                else if (o instanceof CharSequence || o instanceof Character)
                    return Long.parseLong(o.toString());
                else
                    throw new IllegalArgumentException();
            }

            case FLOAT: {
                if (o instanceof Boolean)
                    return ((Boolean) o) ? 1.0 : 0.0;
                else if (o instanceof Number)
                    return ((Number) o).doubleValue();
                else if (o instanceof CharSequence || o instanceof Character)
                    return Double.parseDouble(o.toString());
                else
                    throw new IllegalArgumentException();
            }

            case STRING:
                return ObjectUtils.toString(o, "null");

            case JSON: {
                JsonNodeFactory jnf = JsonNodeFactory.instance;
                if (o instanceof Boolean)
                    return jnf.booleanNode((Boolean) o);
                else if (o instanceof Number)
                    return jnf.numberNode(((Number) o).longValue());
                else if (o instanceof CharSequence || o instanceof Character)
                    try {
                        return JsonHelper.parse(o.toString());
                    } catch (Exception e) {
                        return jnf.nullNode();
                    }
                else if (o instanceof JsonNode)
                    return o;
                else if (o instanceof Collection) {
                    ArrayNode an = jnf.arrayNode();
                    for (Object e : (Collection) o) {
                        Object v = ValuesNewAccount.to(e, JSON);
                        an.add(v instanceof JsonNode ? (JsonNode)v : jnf.nullNode());
                    }
                    return an;
                } else if (o instanceof Iterator) {
                    ArrayNode an = jnf.arrayNode();
                    Iterator iter = (Iterator) o;
                    while (iter.hasNext()) {
                        Object v = ValuesNewAccount.to(iter.next(), JSON);
                        an.add(v instanceof JsonNode ? (JsonNode)v : jnf.nullNode());
                    }
                    return an;
                } else if (o instanceof Map) {
                    ObjectNode on = jnf.objectNode();
                    for (Object e0 : ((Map) o).entrySet()) {
                        Map.Entry e = (Map.Entry) e0;
                        Object v = ValuesNewAccount.to(e.getValue(), JSON);
                        on.put(e.getKey().toString(), v instanceof JsonNode ? (JsonNode)v : jnf.nullNode());
                    }
                    return on;
                }
            }
            break;
        }
        return o;
    }

    public static int simpleTypeOf(Object o) {
        if (o == null)
            return NONE;
        else if (o instanceof Boolean)
            return BOOLEAN;
        else if (o instanceof Integer || o instanceof Long || o instanceof Byte || o instanceof Short)
            return INT;
        else if (o instanceof Double || o instanceof Float)
            return FLOAT;
        else if (o instanceof CharSequence || o instanceof Character)
            return STRING;
        else
            throw new IllegalArgumentException("Values.trimSimple error");
    }

    public static Object trimSimple(Object o) {
        if (o == null || o instanceof Boolean)
            return o;
        else if (o instanceof Integer || o instanceof Long || o instanceof Byte || o instanceof Short)
            return ((Number) o).longValue();
        else if (o instanceof Double || o instanceof Float)
            return ((Number) o).doubleValue();
        else if (o instanceof CharSequence || o instanceof Character)
            return o.toString();
        else
            throw new IllegalArgumentException("Values.trimSimple error");
    }

    public static boolean toBoolean(Object o) {
        return (Boolean) to(o, BOOLEAN);
    }

    public static long toInt(Object o) {
        return (Long) to(o, INT);
    }

    public static double toFloat(Object o) {
        return (Float) to(o, FLOAT);
    }

    public static String toString(Object o) {
        return (String) to(o, STRING);
    }

    public static JsonNode toJson(Object o) {
        return (JsonNode) to(o, JSON);
    }
}
