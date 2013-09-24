package com.borqs.server.base.data;


import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializableWithType;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;

public final class Null implements JsonSerializableWithType {
    public static final Null INSTANCE = new Null();

    private Null() {
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public void serializeWithType(JsonGenerator jsonGenerator, SerializerProvider serializerProvider, TypeSerializer typeSerializer) throws IOException, JsonProcessingException {
        jsonWrite(jsonGenerator);
    }

    @Override
    public void serialize(JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        jsonWrite(jsonGenerator);
    }

    private void jsonWrite(JsonGenerator jg) throws IOException {
        jg.writeNull();
    }
}
