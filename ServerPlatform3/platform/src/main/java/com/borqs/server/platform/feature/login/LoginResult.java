package com.borqs.server.platform.feature.login;


import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.lang.ObjectUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializableWithType;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;

public class LoginResult implements JsonSerializableWithType {
    public final long userId;
    public final String ticket;

    public LoginResult(long userId, String ticket) {
        this.userId = userId;
        this.ticket = ticket;
    }

    @Override
    public String toString() {
        return JsonHelper.toJson(this, true);
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        jg.writeStartObject();
        jg.writeNumberField("user_id", userId);
        jg.writeStringField("ticket", ObjectUtils.toString(ticket));
        jg.writeEndObject();
    }
}
