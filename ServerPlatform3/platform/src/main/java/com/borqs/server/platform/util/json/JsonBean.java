package com.borqs.server.platform.util.json;


import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonSerializableWithType;

public interface JsonBean extends JsonSerializableWithType {
    void deserialize(JsonNode jn);
}
