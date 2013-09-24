package com.borqs.server.platform.feature.link;

import com.borqs.server.platform.util.json.JsonBean;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class LinkEntities extends ArrayList<LinkEntity> implements JsonBean {
    public LinkEntities() {
    }

    public LinkEntities(int initialCapacity) {
        super(initialCapacity);
    }

    public LinkEntities(Collection<? extends LinkEntity> c) {
        super(c);
    }

    public LinkEntities(LinkEntity... les) {
        Collections.addAll(this, les);
    }

    public LinkEntities[] toArray() {
        return toArray(new LinkEntities[size()]);
    }

    @Override
    public void deserialize(JsonNode jn) {
        clear();
        for (int i = 0; i < jn.size(); i++)
            add(LinkEntity.fromJsonNode(jn.path(i)));
    }

    public static LinkEntities fromJson(String json) {
        return fromJsonNode(JsonHelper.parse(json));
    }

    public static LinkEntities fromJsonNode(JsonNode jn) {
        LinkEntities les = new LinkEntities();
        les.deserialize(jn);
        return les;
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        serialize(jg);
    }

    public void serialize(JsonGenerator jg) throws IOException {
        jg.writeStartArray();
        for (LinkEntity le : this) {
            if (le != null)
                le.serialize(jg);
        }
        jg.writeEndArray();
    }

    public String toJson(boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serialize(jg);
            }
        }, human);
    }

    @Override
    public String toString() {
        return toJson(true);
    }
}
