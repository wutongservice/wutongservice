package com.borqs.server.market.models;


import com.borqs.server.market.utils.JsonUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

public class MLTexts {

    public static String trimToText(JsonNode mlNode) {
        if (mlNode == null || mlNode.isNull() || mlNode.isMissingNode())
            return "";
        if (mlNode.isTextual()) {
            return mlNode.asText();
        } else {
            return JsonUtils.toJson(mlNode, false);
        }
    }

    public static JsonNode trimNode(JsonNode mlNode, String defaultLocale) {
        if (mlNode == null || mlNode.isNull() || mlNode.isMissingNode())
            return null;

        if (mlNode.isTextual()) {
            return makeNode(mlNode.asText(), defaultLocale);
        } else if (mlNode.isObject()) {
            return mlNode;
        } else {
            return null;
        }
    }

    public static JsonNode makeNode(String text, String locale) {
        ObjectNode mlNode = JsonNodeFactory.instance.objectNode();
        mlNode.put(locale, text);
        return mlNode;
    }
}
