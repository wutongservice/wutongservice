package com.borqs.server.wutong.account2.util.json;


import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonSerializableWithType;

public interface JsonBean extends JsonSerializableWithType {
    void deserialize(JsonNode jn);
}
