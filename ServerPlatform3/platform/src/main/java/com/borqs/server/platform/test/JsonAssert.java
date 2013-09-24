package com.borqs.server.platform.test;

import com.borqs.server.platform.util.json.JsonCompare;
import org.codehaus.jackson.JsonNode;

import static org.junit.Assert.assertTrue;

public class JsonAssert {

    public static void assertJsonEquals(JsonNode node1, JsonNode node2) {
        JsonCompare.Result r = JsonCompare.compare(node1, node2);
        assertTrue(r.toString(), r.isEquals());
    }

    public static void assertJsonObjectEquals(JsonNode node1, JsonNode node2) {
        assertJsonObjectEquals(node1, node2, new String[0]);
    }

    public static void assertJsonObjectEquals(JsonNode node1, JsonNode node2, String[] excludedFields) {
        JsonCompare.Result r = JsonCompare.compareObject(node1, node2, excludedFields);
        assertTrue(r.toString(), r.isEquals());
    }

    public static void assertJsonObjectArrayEquals(JsonNode node1, JsonNode node2) {
        assertJsonObjectArrayEquals(node1, node2, null, new String[0]);
    }

    public static void assertJsonObjectArrayEquals(JsonNode node1, JsonNode node2, String[] excludedFields) {
        assertJsonObjectArrayEquals(node1, node2, null, excludedFields);
    }

    public static void assertJsonObjectArrayEquals(JsonNode node1, JsonNode node2, JsonCompare.ArrayRange range) {
        assertJsonObjectArrayEquals(node1, node2, range, new String[0]);
    }

    public static void assertJsonObjectArrayEquals(JsonNode node1, JsonNode node2, JsonCompare.ArrayRange range, String[] excludedFields) {
        JsonCompare.Result r = JsonCompare.compareObjectArray(node1, node2, range, excludedFields);
        assertTrue(r.toString(), r.isEquals());
    }

}
