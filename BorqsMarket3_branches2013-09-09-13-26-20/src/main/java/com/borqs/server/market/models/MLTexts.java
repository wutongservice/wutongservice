package com.borqs.server.market.models;


import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

public class MLTexts {
    public static JsonNode trimNode(JsonNode mlNode, String defaultLocale) {
        if (mlNode == null)
            return null;

        if (mlNode.isTextual()) {
            return makeNode(mlNode.asText(), defaultLocale);
        } else if (mlNode.isObject()) {
            return mlNode;
        } else if (mlNode.isNull() || mlNode.isMissingNode()) {
            return makeNode("", defaultLocale);
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
