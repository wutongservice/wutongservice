package com.borqs.server.market.models;


import com.borqs.server.market.utils.JsonUtils;
import com.borqs.server.market.utils.ObjectUtils2;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializableWithType;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;

public class VersionedProductId implements JsonSerializableWithType {
    private String id;
    private int version;

    public VersionedProductId() {
        this(null, 0);
    }

    public VersionedProductId(String id, int version) {
        this.id = id;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        VersionedProductId that = (VersionedProductId) o;
        return StringUtils.equals(id, that.id) && version == that.version;
    }

    @Override
    public int hashCode() {
        return ObjectUtils2.hashCodeMulti(id, version);
    }

    @Override
    public String toString() {
        return ObjectUtils.toString(id) + ":" + version;
    }

    @Override
    public void serializeWithType(JsonGenerator jgen, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jgen, provider);
    }

    @Override
    public void serialize(JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        jgen.writeStringField("id", id);
        jgen.writeNumberField("version", version);
        jgen.writeEndObject();
    }

    public static VersionedProductId fromJsonNode(JsonNode jn) {
        return new VersionedProductId(jn.path("id").asText(), jn.path("version").asInt());
    }

    public static VersionedProductId parseJson(String json) throws IOException {
        return fromJsonNode(JsonUtils.parseJson(json));
    }
}
