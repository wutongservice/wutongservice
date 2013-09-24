package com.borqs.server.platform.feature;


import com.borqs.server.platform.util.ObjectHelper;
import com.borqs.server.platform.util.json.JsonBean;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.util.ArrayList;

public class TargetInfo implements JsonBean {
    public static final String COL_TARGET = "target";
    public static final String COL_NAME = "name";
    public static final String COL_IMAGE_URL = "image_url";

    private Target target;
    private String name;
    private String imageUrl;

    public TargetInfo() {
        this(Target.NONE_ID, "", "");
    }

    public TargetInfo(Target target, String name, String imageUrl) {
        this.target = target;
        this.name = name;
        this.imageUrl = imageUrl;
    }

    public static TargetInfo of(Target target, String name, String imageUrl) {
        return new TargetInfo(target, name, imageUrl);
    }

    public static TargetInfo of(Target target, String name) {
        return new TargetInfo(target, name, "");
    }

    public static TargetInfo of(String target, String name, String imageUrl) {
        return new TargetInfo(Target.parseCompatibleString(target), name, imageUrl);
    }

    public static TargetInfo of(String target, String name) {
        return new TargetInfo(Target.parseCompatibleString(target), name, "");
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public static TargetInfo[] arrayFromJson(String json) {
        return arrayFromJsonNode(JsonHelper.parse(json));
    }

    public static TargetInfo[] arrayFromJsonNode(JsonNode jn) {
        if (jn.isObject()) {
            return new TargetInfo[] { fromJsonNode(jn)};
        } else {
            ArrayList<TargetInfo> l = new ArrayList<TargetInfo>();
            for (int i = 0; i < jn.size(); i++)
                l.add(TargetInfo.fromJsonNode(jn.get(i)));
            return l.toArray(new TargetInfo[l.size()]);
        }
    }

    public static TargetInfo fromJson(String json) {
        return fromJsonNode(JsonHelper.parse(json));
    }

    public static TargetInfo fromJsonNode(JsonNode jn) {
        TargetInfo info = new TargetInfo();
        info.deserialize(jn);
        return info;
    }

    @Override
    public void deserialize(JsonNode jn) {
        if (jn.has(COL_TARGET))
            setTarget(Target.parseCompatibleString(jn.get(COL_TARGET).getValueAsText()));
        if (jn.has(COL_NAME))
            setName(jn.get(COL_NAME).getValueAsText());
        if (jn.has(COL_IMAGE_URL))
            setImageUrl(jn.get(COL_IMAGE_URL).getValueAsText());
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
        jg.writeStartObject();
        if (cols == null || ArrayUtils.contains(cols, COL_TARGET))
            jg.writeStringField(COL_TARGET, target == null ? "" : target.toCompatibleString());
        if (cols == null || ArrayUtils.contains(cols, COL_NAME))
            jg.writeStringField(COL_NAME, ObjectUtils.toString(name));
        if (cols == null || ArrayUtils.contains(cols, COL_IMAGE_URL))
            jg.writeStringField(COL_IMAGE_URL, ObjectUtils.toString(imageUrl));
        jg.writeEndObject();
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

    public static String arrayToJson(final TargetInfo[] targetInfo, final String[] cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                jg.writeStartArray();
                for (TargetInfo ti : targetInfo)
                    ti.serialize(jg, cols);
                jg.writeEndArray();
            }
        }, human);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        TargetInfo other = (TargetInfo) o;
        return ObjectUtils.equals(target, other.target)
                && StringUtils.equals(name, other.name)
                && StringUtils.equals(imageUrl, other.imageUrl);
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(target, name, imageUrl);
    }
}
