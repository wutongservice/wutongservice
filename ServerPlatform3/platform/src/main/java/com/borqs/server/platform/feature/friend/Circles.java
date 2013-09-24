package com.borqs.server.platform.feature.friend;


import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.json.JsonBean;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.lang.ArrayUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializableWithType;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class Circles extends ArrayList<Circle> implements JsonBean {
    public Circles() {
    }

    public Circles(int initialCapacity) {
        super(initialCapacity);
    }

    public Circles(Collection<? extends Circle> c) {
        super(c);
    }

    public int[] getCircleIds() {
        ArrayList<Integer> l = new ArrayList<Integer>();
        for (Circle circle : this)
            l.add(circle.getCircleId());
        return CollectionsHelper.toIntArray(l);
    }

    public int[] getCircleIds(int minCircleId) {
        ArrayList<Integer> l = new ArrayList<Integer>();
        for (Circle circle : this) {
            if (circle.getCircleId() >= minCircleId)
                l.add(circle.getCircleId());
        }
        return CollectionsHelper.toIntArray(l);
    }

    public Circle getCircle(int circleId) {
        for (Circle circle : this) {
            if (circle != null && circle.getCircleId() == circleId)
                return circle;
        }
        return null;
    }

    public Circles getCircles(int[] circleIds, boolean copy) {
        Circles cc = new Circles();
        if (ArrayUtils.isNotEmpty(circleIds)) {
            for (int circleId : circleIds) {
                Circle c = getCircle(circleId);
                if (c != null)
                    cc.add(copy ? c.copy() : c);
            }
        }
        return cc;
    }

    public boolean hasCircle(int circleId) {
        return getCircle(circleId) != null;
    }

    @Override
    public void deserialize(JsonNode jn) {
        for (int i = 0; i < jn.size(); i++) {
            Circle circle = Circle.fromJsonNode(jn.path(i));
            add(circle);
        }
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        serialize(jg, (String[])null);
    }

    public void serialize(JsonGenerator jg, String[] cols) throws IOException {
        jg.writeStartArray();
        for (Circle c : this) {
            if (c != null)
                c.serialize(jg, cols);
        }
        jg.writeEndArray();
    }

    @Override
    public String toString() {
        return toJson(Circle.CIRCLE_COLUMNS, true);
    }

    public String toJson(final String[] cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serialize(jg, cols);
            }
        }, human);
    }

    public static Circles fromJsonNode(Circles reuse, JsonNode jn) {
        Circles circles = reuse != null ? reuse : new Circles();
        circles.deserialize(jn);
        return circles;
    }


    public static Circles fromJson(Circles reuse, String json) {
        return fromJsonNode(reuse, JsonHelper.parse(json));
    }

    public PeopleIds getAllFriendIds() {
        PeopleIds pids = new PeopleIds();
        for (Circle circle : this) {
            if (circle != null && circle.getFriendIds() != null)
                pids.addAll(circle.getFriendIds());
        }
        return pids;
    }
}
