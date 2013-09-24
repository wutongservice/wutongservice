package com.borqs.server.platform.util.json;


import com.borqs.server.E;
import com.borqs.server.ServerException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import java.io.IOException;
import java.util.Arrays;
import java.util.TreeSet;

public class JsonCompare {
    private static JsonNodeFactory JNF = JsonNodeFactory.instance;

    private static JsonNode trimToNull(JsonNode node) {
        return node != null ? node : JsonNodeFactory.instance.nullNode();
    }

    public static Result compare(JsonNode node1, JsonNode node2) {
        node1 = trimToNull(node1);
        node2 = trimToNull(node2);

        if (node1.isNull() && node2.isNull()) {
            return new EQ(node1, node2);
        } else if (node1.isTextual() && node2.isTextual()) {
            return StringUtils.equals(node1.getTextValue(), node2.getTextValue()) ? new EQ(node1, node2) : new ValueDiff(node1, node2);
        } else if (node1.isBoolean() && node2.isBoolean()) {
            return node1.getBooleanValue() == node2.getBooleanValue() ? new EQ(node1, node2) : new ValueDiff(node1, node2);
        } else if (node1.isNumber() && node2.isNumber()) {
            if (node1.isIntegralNumber() && node2.isIntegralNumber()) {
                return node1.getValueAsLong() == node2.getValueAsLong() ? new EQ(node1, node2) : new ValueDiff(node1, node2);
            } else {
                return Double.compare(node1.getValueAsDouble(), node2.getValueAsDouble()) == 0 ? new EQ(node1, node2) : new ValueDiff(node1, node2);
            }
        } else if (node1.isBinary() && node2.isBinary()) {
            try {
                return Arrays.equals(node1.getBinaryValue(), node2.getBinaryValue()) ? new EQ(node1, node2) : new ValueDiff(node1, node2);
            } catch (IOException e) {
                throw new ServerException(E.JSON, e);
            }
        } else if (node1.isObject() && node2.isObject()) {
            return compareObject(node1, node2);
        } else if (node1.isArray() && node2.isArray()) {
            return compareArray(node1, node2);
        } else {
            return new TypeDiff(node1, node2);
        }
    }

    public static Result compareType(JsonNode node1, JsonNode node2) {
        node1 = trimToNull(node1);
        node2 = trimToNull(node2);

        boolean typeEq = (node1.isNull() && node2.isNull())
                || (node1.isTextual() && node2.isTextual())
                || (node1.isNumber() && node2.isNumber())
                || (node1.isObject() && node2.isObject())
                || (node1.isArray() && node2.isArray())
                || (node1.isBinary() && node2.isBinary());
        return typeEq ? new EQ(node1, node2) : new TypeDiff(node1, node2);
    }


    public static Result compareObject(JsonNode node1, JsonNode node2) {
        return compareObject(node1, node2, null);
    }

    public static Result compareObject(JsonNode node1, JsonNode node2, String[] excludedFields) {
        node1 = trimToNull(node1);
        node2 = trimToNull(node2);
        if (!node1.isObject() || !node2.isObject())
            return new TypeError(node1, node2);

        if (excludedFields == null)
            excludedFields = new String[0];

        TreeSet<String> allFields = new TreeSet<String>();
        allFields.addAll(JsonHelper.getObjectFields(node1));
        allFields.addAll(JsonHelper.getObjectFields(node2));
        for (String f : allFields) {
            if (ArrayUtils.contains(excludedFields, f))
                continue;

            if (!(node1.has(f) && node2.has(f)))
                return new ObjectDiff(node1, node2, excludedFields);

            if (!compare(node1.get(f), node2.get(f)).isEquals())
                return new ObjectDiff(node1, node2, excludedFields);
        }
        return new EQ(node1, node2);
    }

    public static Result compareArray(JsonNode node1, JsonNode node2) {
        return compareArray(node1, node2, null);
    }

    public static Result compareArray(JsonNode node1, JsonNode node2, ArrayRange range) {
        return compareArrayHelper(node1, node2, range, null, false);
    }

    public static Result compareObjectArray(JsonNode node1, JsonNode node2) {
        return compareObjectArray(node1, node2, null, new String[0]);
    }

    public static Result compareObjectArray(JsonNode node1, JsonNode node2, ArrayRange range) {
        return compareObjectArray(node1, node2, range, new String[0]);
    }

    public static Result compareObjectArray(JsonNode node1, JsonNode node2, String[] excludedFields) {
        return compareObjectArray(node1, node2, null, excludedFields);
    }

    public static Result compareObjectArray(JsonNode node1, JsonNode node2, ArrayRange range, String[] excludedFields) {
        return compareArrayHelper(node1, node2, range, excludedFields, true);
    }

    public static Result compareArrayHelper(JsonNode node1, JsonNode node2, ArrayRange range, String[] excludedFields, boolean forObject) {
        node1 = trimToNull(node1);
        node2 = trimToNull(node2);
        if (!node1.isArray() || !node2.isArray())
            return new TypeError(node1, node2);

        if (range == null)
            range = ArrayRange.maxLengthRange(node1, node2);


        for (int i = range.from; i < range.to; i++) {
            if (!(node1.has(i) && node2.has(i)))
                return new ArrayDiff(node1, node2, range);

            if (forObject) {
                if (!compareObject(node1.get(i), node2.get(i), excludedFields).isEquals())
                    return new ArrayDiff(node1, node2, true, range);
            } else {
                if (!compare(node1.get(i), node2.get(i)).isEquals())
                    return new ArrayDiff(node1, node2, false, range);
            }
        }
        return new EQ(node1, node2);
    }

    public static class ArrayRange {
        public final int from;
        public final int to;

        private ArrayRange(int from, int to) {
            this.from = from;
            this.to = to;
        }

        public static ArrayRange of(int from, int to) {
            return new ArrayRange(from, to);
        }

        public int length() {
            return to - from;
        }

        public static ArrayRange maxLengthRange(JsonNode node1, JsonNode node2) {
            int maxLen = node1.size();
            if (node2.size() > maxLen)
                maxLen = node2.size();
            return new ArrayRange(0, maxLen);
        }
    }


    private static final String MISSING = "#";

    private static JsonNode newValuePairNode(JsonNode n1, JsonNode n2) {
        ObjectNode on = JNF.objectNode();
        on.put("1", n1);
        on.put("2", n2);
        return on;
    }

    private static JsonNode newResultNode(String r) {
        ObjectNode on = JNF.objectNode();
        on.put("result", r);
        return on;
    }

    private static JsonNode newResultNode(String r, JsonNode n1, JsonNode n2) {
        ObjectNode on = JNF.objectNode();
        on.put("result", r);
        on.put("diff", newValuePairNode(n1, n2));
        return on;
    }

    private static JsonNode newResultNode(String r, JsonNode diff) {
        ObjectNode on = JNF.objectNode();
        on.put("result", r);
        on.put("diff", diff);
        return on;
    }

    private static JsonNode newEqElementNode(int index) {
        ObjectNode on = JNF.objectNode();
        on.put("index", index);
        on.put("result", "EQ");
        return on;
    }

    private static JsonNode newNeElementNode(int index, JsonNode n1, JsonNode n2) {
        ObjectNode on = JNF.objectNode();
        on.put("index", index);
        on.put("1", n1);
        on.put("2", n2);
        return on;
    }

    public static abstract class Result {
        public final JsonNode node1;
        public final JsonNode node2;

        protected Result(JsonNode node1, JsonNode node2) {
            this.node1 = node1;
            this.node2 = node2;
        }

        public abstract boolean isEquals();

        @Override
        public String toString() {
            return JsonHelper.toJson(toJsonNode(), true);
        }

        public abstract JsonNode toJsonNode();
    }

    public static class EQ extends Result {
        private EQ(JsonNode node1, JsonNode node2) {
            super(node1, node2);
        }

        @Override
        public boolean isEquals() {
            return true;
        }

        @Override
        public JsonNode toJsonNode() {
            return newResultNode("EQ");
        }
    }

    public static class TypeDiff extends Result {
        private TypeDiff(JsonNode node1, JsonNode node2) {
            super(node1, node2);
        }

        @Override
        public boolean isEquals() {
            return false;
        }

        @Override
        public JsonNode toJsonNode() {
            return newResultNode("NE - Different type", node1, node2);
        }
    }

    public static class TypeError extends Result {
        private TypeError(JsonNode node1, JsonNode node2) {
            super(node1, node2);
        }

        @Override
        public boolean isEquals() {
            return false;
        }

        @Override
        public JsonNode toJsonNode() {
            return newResultNode("NE - Illegal type", node1, node2);
        }
    }

    public static class ValueDiff extends Result {
        private ValueDiff(JsonNode node1, JsonNode node2) {
            super(node1, node2);
        }

        @Override
        public boolean isEquals() {
            return false;
        }

        @Override
        public JsonNode toJsonNode() {
            return newResultNode("NE - Different value", node1, node2);
        }
    }


    public static class ObjectDiff extends Result {
        public final String[] excludedFields;

        public ObjectDiff(JsonNode node1, JsonNode node2, String[] excludedFields) {
            super(node1, node2);
            this.excludedFields = excludedFields;
        }

        @Override
        public boolean isEquals() {
            return false;
        }

        @Override
        public JsonNode toJsonNode() {
            ObjectNode diff = JNF.objectNode();
            TreeSet<String> allFields = new TreeSet<String>();
            allFields.addAll(JsonHelper.getObjectFields(node1));
            allFields.addAll(JsonHelper.getObjectFields(node2));
            for (String f : allFields) {
                if (ArrayUtils.contains(excludedFields, f))
                    diff.put(f, "Ignored");
                boolean has1 = node1.has(f);
                boolean has2 = node2.has(f);
                if (has1 && has2) {
                    JsonNode e1 = node1.get(f);
                    JsonNode e2 = node2.get(f);
                    Result fcr = compare(e1, e2);
                    if (fcr.isEquals()) {
                        diff.put(f, "EQ");
                    } else {
                        diff.put(f, newValuePairNode(e1, e2));
                    }
                } else if (has1) {
                    JsonNode e1 = node1.get(f);
                    diff.put(f, newValuePairNode(e1, JNF.textNode(MISSING)));
                } else { // has2
                    JsonNode e2 = node2.get(f);
                    diff.put(f, newValuePairNode(JNF.textNode(MISSING), e2));
                }
            }
            return newResultNode("NE - Different object", diff);
        }
    }

    public static class ArrayDiff extends Result {
        public final boolean forObject;
        public final ArrayRange range;

        private ArrayDiff(JsonNode node1, JsonNode node2, ArrayRange range) {
            this(node1, node2, false, range);
        }

        private ArrayDiff(JsonNode node1, JsonNode node2, boolean forObject, ArrayRange range) {
            super(node1, node2);
            this.forObject = forObject;
            this.range = range;
        }

        @Override
        public boolean isEquals() {
            return false;
        }

        @Override
        public JsonNode toJsonNode() {
            ArrayNode diff = JNF.arrayNode();
            for (int i = range.from; i < range.to; i++) {
                boolean has1 = node1.has(i);
                boolean has2 = node2.has(i);
                if (has1 && has2) {
                    JsonNode e1 = node1.get(i);
                    JsonNode e2 = node2.get(i);
                    Result ecr = compare(e1, e2);
                    if (ecr.isEquals()) {
                        diff.add(newEqElementNode(i));
                    } else {
                        diff.add(newNeElementNode(i, e1, e2));
                    }
                } else if (has1) {
                    JsonNode e1 = node1.get(i);
                    diff.add(newNeElementNode(i, e1, JNF.textNode(MISSING)));
                } else { // has2
                    JsonNode e2 = node2.get(i);
                    diff.add(newNeElementNode(i, JNF.textNode(MISSING), e2));
                }
            }

            return newResultNode(forObject ? "NE - Different object array" : "NE - Different array", diff);
        }
    }
}

