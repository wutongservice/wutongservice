package com.borqs.server.base.util.json;


import com.borqs.server.base.data.DataException;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;
import org.codehaus.jackson.util.DefaultPrettyPrinter;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;

public class JsonUtils {

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

    public static String toJson(Object o, boolean human) {
        StringWriter w = new StringWriter();
        JsonGenerator jg = null;
        try {
            try {
                jg = JSON_FACTORY.createJsonGenerator(w);
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
            throw new JsonException(e);
        }
    }


    public static String toJson(JsonGenerateHandler h, boolean human) {
        StringWriter w = new StringWriter();
        JsonGenerator jg = null;
        try {
            try {
                jg = JSON_FACTORY.createJsonGenerator(w);
                if (human)
                    jg.setPrettyPrinter(new DefaultPrettyPrinter());

                h.generate(jg);
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
            throw new JsonException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String json, Class<T> type) {
        return (T)fromJson(json, (Type)type);
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
            throw new JsonException(e);
        }
    }

    public static JsonNode parse(String json) {
        return fromJson(json, JsonNode.class);
    }

    public static boolean isValidate(String json) {
        try {
            // TODO: this method is slow
            parse(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static ObjectNode checkRecord(JsonNode jn) {
        if (!jn.isObject())
            throw new JsonException("Json node is not a record");
        return (ObjectNode)jn;
    }

    public static ArrayNode checkRecordSet(JsonNode jn) {
        if (!jn.isArray())
            throw new DataException("Json node is not a record set");

        for (int i = 0; i < jn.size(); i++) {
            if (!jn.get(i).isObject())
                throw new JsonException("Json node is not a record set");
        }
        return (ArrayNode)jn;
    }
}
