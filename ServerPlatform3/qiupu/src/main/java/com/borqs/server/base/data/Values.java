package com.borqs.server.base.data;


import com.borqs.server.base.util.json.JsonUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class Values {

    public static final Null NULL = Null.INSTANCE;

    public static final Privacy PRIVACY = Privacy.INSTANCE;

    public static DataType parseType(String type) {
        if (type.equalsIgnoreCase("boolean") || type.equalsIgnoreCase("bool"))
            return DataType.BOOLEAN;
        if (type.equalsIgnoreCase("integer") || type.equalsIgnoreCase("int"))
            return DataType.INT;
        if (type.equalsIgnoreCase("float"))
            return DataType.FLOAT;
        if (type.equalsIgnoreCase("string") || type.equalsIgnoreCase("str"))
            return DataType.STRING;
        return DataType.NULL;
    }

    public static Object to(Object o, DataType type) {
        Validate.notNull(type);

        if (o == null)
            return Values.NULL;

        if (o instanceof Null || o instanceof Privacy)
            return o;

        switch (type) {
            case BOOLEAN: {
                if (o instanceof Boolean)
                    return o;
                else if (o instanceof Number)
                    return ((Number) o).longValue() != 0L;
                else if (o instanceof CharSequence)
                    return Boolean.parseBoolean(o.toString());
                else if (o instanceof JsonNode) {
                    JsonNode jn = (JsonNode) o;
                    if (jn.isBoolean())
                        return jn.getBooleanValue();
                    else if (jn.isNumber())
                        return jn.getNumberValue().longValue() != 0L;
                    else if (jn.isTextual())
                        return Boolean.parseBoolean(jn.getTextValue());
                }
            }
            break;

            case INT: {
                if (o instanceof Boolean)
                    return ((Boolean) o) ? 1L : 0L;
                else if (o instanceof Number)
                    return ((Number) o).longValue();
                else if (o instanceof CharSequence || o instanceof Character)
                    return Long.parseLong(o.toString());
                else if (o instanceof JsonNode) {
                    JsonNode jn = (JsonNode) o;
                    if (jn.isBoolean())
                        return jn.getBooleanValue() ? 1L : 0L;
                    else if (jn.isNumber())
                        return jn.getNumberValue().longValue();
                    else if (jn.isTextual())
                        return Long.parseLong(jn.getTextValue());
                }
            }
            break;

            case FLOAT: {
                if (o instanceof Boolean)
                    return ((Boolean) o) ? 1.0 : 0.0;
                else if (o instanceof Number)
                    return ((Number) o).doubleValue();
                else if (o instanceof CharSequence || o instanceof Character)
                    return Double.parseDouble(o.toString());
                else if (o instanceof JsonNode) {
                    JsonNode jn = (JsonNode) o;
                    if (jn.isBoolean())
                        return jn.getBooleanValue() ? 1.0 : 0.0;
                    else if (jn.isNumber())
                        return jn.getNumberValue().doubleValue();
                    else if (jn.isTextual())
                        return Double.parseDouble(jn.getTextValue());
                }
            }
            break;

            case STRING: {
                if (o instanceof JsonNode) {
                    JsonNode jn = (JsonNode)o;
                    if (jn.isBoolean())
                        return Boolean.toString(jn.getBooleanValue());
                    else if (jn.isIntegralNumber())
                        return Long.toString(jn.getLongValue());
                    else if (jn.isFloatingPointNumber())
                        return Double.toString(jn.getDoubleValue());
                    else if (jn.isTextual())
                        return jn.getTextValue();
                    else
                        return jn.toString();
                } else {
                    return ObjectUtils.toString(o, "");
                }
            }

            case JSON: {
                JsonNodeFactory jnf = JsonNodeFactory.instance;
                if (o instanceof Boolean)
                    return jnf.booleanNode((Boolean) o);
                else if (o instanceof Number)
                    return jnf.numberNode(((Number) o).longValue());
                else if (o instanceof CharSequence || o instanceof Character)
                    try {
                        return JsonUtils.parse(o.toString());
                    } catch (Exception e) {
                        return jnf.nullNode();
                    }
                else if (o instanceof JsonNode)
                    return o;
                else if (o instanceof Collection) {
                    ArrayNode an = jnf.arrayNode();
                    for (Object e : (Collection) o) {
                        Object v = Values.to(e, DataType.JSON);
                        an.add(v instanceof JsonNode ? (JsonNode)v : jnf.nullNode());
                    }
                    return an;
                } else if (o instanceof Iterator) {
                    ArrayNode an = jnf.arrayNode();
                    Iterator iter = (Iterator) o;
                    while (iter.hasNext()) {
                        Object v = Values.to(iter.next(), DataType.JSON);
                        an.add(v instanceof JsonNode ? (JsonNode)v : jnf.nullNode());
                    }
                    return an;
                } else if (o instanceof Map) {
                    ObjectNode on = jnf.objectNode();
                    for (Object e0 : ((Map) o).entrySet()) {
                        Map.Entry e = (Map.Entry) e0;
                        Object v = Values.to(e.getValue(), DataType.JSON);
                        on.put(e.getKey().toString(), v instanceof JsonNode ? (JsonNode)v : jnf.nullNode());
                    }
                    return on;
                }
            }
            break;
        }
        throw new IllegalArgumentException();
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
        return (Boolean)to(o, DataType.BOOLEAN);
    }

    public static long toInt(Object o) {
        return (Long)to(o, DataType.INT);
    }

    public static double toFloat(Object o) {
        return (Float)to(o, DataType.FLOAT);
    }

    public static String toString(Object o) {
        return (String)to(o, DataType.STRING);
    }
}
