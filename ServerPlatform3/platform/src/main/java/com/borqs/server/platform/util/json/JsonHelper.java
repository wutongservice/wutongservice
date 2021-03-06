package com.borqs.server.platform.util.json;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.type.TypeReference;
import org.codehaus.jackson.util.DefaultPrettyPrinter;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class JsonHelper {

    private static final JsonFactory JSON_FACTORY = new JsonFactory();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static Object readValue(JsonParser jp, final Type type) throws IOException {
        return OBJECT_MAPPER.readValue(jp, new TypeReference<Object>() {
            @Override
            public Type getType() {
                return type;
            }
        });
    }

    public static void writeValue(JsonGenerator jg, Object o, Boolean human) throws IOException {

        if (human != null && human)
            jg.setPrettyPrinter(new DefaultPrettyPrinter());

        OBJECT_MAPPER.writeValue(jg, o);
    }

    public static JsonGenerator jsonGenerator(Writer out) throws IOException {
        JsonGenerator jg = JSON_FACTORY.createJsonGenerator(out);
        jg.setCodec(OBJECT_MAPPER);
        return jg;
    }

    public static JsonGenerator jsonGenerator(OutputStream out) throws IOException {
        JsonGenerator jg = JSON_FACTORY.createJsonGenerator(out);
        jg.setCodec(OBJECT_MAPPER);
        return jg;
    }


    public static String toJson(Object o, boolean human) {
        StringWriter w = new StringWriter();
        JsonGenerator jg = null;
        try {
            try {
                jg = jsonGenerator(w);
                writeValue(jg, o, human);
                jg.flush();
                return w.toString();
            } finally {
                if (jg != null) {
                    try {
                        jg.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (IOException e) {
            throw new ServerException(E.JSON, e);
        }
    }

    public static boolean isJson(String s) {
        if (StringUtils.isBlank(s))
            return false;

        try {
            JsonParser parser = JSON_FACTORY.createJsonParser(s);
            while (parser.nextToken() != null) {
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void toJson(Writer out, JsonGenerateHandler h, boolean human) {
        JsonGenerator jg = null;
        try {
            try {
                jg = jsonGenerator(out);
                if (human)
                    jg.setPrettyPrinter(new DefaultPrettyPrinter());

                h.generate(jg, null);
                jg.flush();
            } finally {
                if (jg != null) {
                    try {
                        jg.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (IOException e) {
            throw new ServerException(E.JSON, e);
        }
    }



    public static String toJson(JsonGenerateHandler h, boolean human) {
        StringWriter out = new StringWriter();
        toJson(out, h, human);
        return out.toString();
    }

    public static JsonNode toJsonNode(JsonGenerateHandler h) {
        String json = toJson(h, false);
        return parse(json);
    }

    public static <T> T fromJsonNode(JsonNode jn, Class<T> type) {
        Validate.notNull(type);
        if (jn == null)
            return null;

        try {
            return OBJECT_MAPPER.readValue(jn, type);
        } catch (IOException e) {
            throw new ServerException(E.JSON, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String json, Class<T> type) {
        return (T) fromJson(json, (Type) type);
    }

    public static Object fromJson(String json, Type type) {
        Validate.notNull(json);
        Validate.notNull(type);

        JsonParser jp = null;
        try {
            try {
                jp = JSON_FACTORY.createJsonParser(json);
                return readValue(jp, type);
            } finally {
                if (jp != null) {
                    try {
                        jp.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (IOException e) {
            throw new ServerException(E.JSON, e);
        }
    }

    public static JsonNode parse(String json) {
        return fromJson(json, JsonNode.class);
    }

    public static Set<String> getObjectFields(JsonNode node) {
        LinkedHashSet<String> fields = new LinkedHashSet<String>();
        if (node != null && node.isObject()) {
            Iterator<String> iter = node.getFieldNames();
            while (iter.hasNext())
                fields.add(iter.next());
        }
        return fields;
    }

    public static JsonNode getArrayElement(JsonNode node, int index) {
        if (node == null || !node.isArray())
            return null;

        return (index >= 0 && index < node.size()) ? node.get(index) : null;
    }

    public static Object trimSimpleJsonNode(JsonNode jn) {
        if (jn == null || jn.isNull() || jn.isMissingNode())
            return null;

        if (jn.isBoolean())
            return jn.getValueAsBoolean();
        else if (jn.isIntegralNumber())
            return jn.getValueAsLong();
        else if (jn.isFloatingPointNumber())
            return jn.getValueAsDouble();
        else if (jn.isTextual())
            return jn.getValueAsText();
        else
            throw new IllegalArgumentException("Illegal simple value json node " + jn.toString());
    }

    public static String[] getFieldNameArray(JsonNode jn) {
        ArrayList<String> l = new ArrayList<String>();
        Iterator<String> iter = jn.getFieldNames();
        while (iter.hasNext()) {
            l.add(iter.next());
        }
        return l.toArray(new String[l.size()]);
    }
}
