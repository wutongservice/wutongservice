package com.borqs.server.market.utils;


import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class JsonUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String toJson(Object data, boolean pretty) {
        StringWriter out = new StringWriter();
        try {
            toJson(out, data, pretty);
        } catch (IOException ignored) {
        }
        return out.toString();
    }

    public static void toJson(Writer out, Object data, boolean pretty) throws IOException {
        if (pretty) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(out, data);
        } else {
            objectMapper.writeValue(out, data);
        }
    }

    public static String toJson(boolean pretty, JsonGeneratorHandler handler) throws IOException {
        StringWriter out = new StringWriter();
        toJson(out, pretty, handler);
        return out.toString();
    }

    public static void toJson(Writer out, boolean pretty, JsonGeneratorHandler handler) throws IOException {
        JsonGenerator jgen = objectMapper.getJsonFactory().createJsonGenerator(out);
        try {
            handler.generate(jgen);
        } finally {
            jgen.close();
        }
    }

    public static JsonNode parseJson(String json) throws IOException {
        JsonParser parser = objectMapper.getJsonFactory().createJsonParser(json);
        return objectMapper.readTree(parser);
    }

    public static JsonNode toJsonNode(Object data) throws IOException {
        return parseJson(toJson(data, false));
    }

    public static JsonNode clone(JsonNode jn){
        if (jn == null) {
            return null;
        } else {
            try {
                return parseJson(toJson(jn, false));
            } catch (IOException e) {
                throw new IllegalArgumentException("Illegal argument for clone json node");
            }
        }
    }

    public static JsonNode asArrayNode(JsonNode jn) {
        if (jn == null)
            return null;

        if (jn.isArray())
            return jn;

        ArrayNode l = JsonNodeFactory.instance.arrayNode();
        l.add(jn);
        return l;
    }
}
