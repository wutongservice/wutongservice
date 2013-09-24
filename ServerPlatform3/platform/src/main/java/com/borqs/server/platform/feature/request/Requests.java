package com.borqs.server.platform.feature.request;

import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializableWithType;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.util.*;

public class Requests extends ArrayList<Request> implements JsonSerializableWithType {

    public Requests() {
    }

    public Requests(int initialCapacity) {
        super(initialCapacity);
    }

    public Requests(Collection<? extends Request> c) {
        super(c);
    }

    public Requests(Request... reqs) {
        Collections.addAll(this, reqs);
    }

    public long[] getToIds() {
        Set<Long> set = new HashSet<Long>();
        for (Request request : this) {
            Long to = request.getTo();
            if (!set.contains(to))
                set.add(to);
        }
        return CollectionsHelper.toLongArray(set);
    }

    public long[] getFromIds() {
        LinkedHashSet<Long> set = new LinkedHashSet<Long>();
        for (Request request : this) {
            if (request != null)
                set.add(request.getFrom());
        }
        return CollectionsHelper.toLongArray(set);
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        serialize(jg, (String[]) null);
    }

    public void serialize(JsonGenerator jg, String[] cols) throws IOException {
        jg.writeStartArray();
        for (Request req : this) {
            if (req != null)
                req.serialize(jg, cols);
        }
        jg.writeEndArray();
    }

    public String toJson(final String[] cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serialize(jg, cols);
            }
        }, human);
    }

    @Override
    public String toString() {
        return toJson(null, true);
    }

    public Request[] toRequestArray() {
        return toArray(new Request[size()]);
    }
}
