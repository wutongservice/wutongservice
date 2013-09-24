package com.borqs.server.platform.feature.ignore;

import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.util.ColumnsExpander;
import com.borqs.server.platform.util.Copyable;
import com.borqs.server.platform.util.ObjectHelper;
import com.borqs.server.platform.util.json.JsonBean;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;

public class Ignore extends Addons implements JsonBean, Copyable<Ignore> {

    public static final String COL_IGNORE_ID = "ignore_id";
    public static final String COL_TARGET = "target";
    public static final String COL_CREATED_TIME = "created_time";
    public static final String COL_DESTROYED_TIME = "destroyed_time";
    public static final String COL_USER = "user_id";
    public static final String COL_FEATURE= "feature";


    private long ignoreId;
    private Target target;
    private long createdTime;
    private long destroyedTime;
    private long userId;
    private int feature;


    public Ignore() {
    }

    public static final String[] STANDARD_COLUMNS = {
            COL_IGNORE_ID,
            COL_TARGET,
            COL_CREATED_TIME,
            COL_DESTROYED_TIME,
            COL_USER,
            COL_FEATURE,
    };
    public static final String[] FULL_COLUMNS = STANDARD_COLUMNS;

    public static String[] expandColumns(String[] cols) {
        return ColumnsExpander.expand(cols,
                "@full,#full", FULL_COLUMNS,
                "@std,#std,@normal,#normal", STANDARD_COLUMNS);
    }

    public Ignore(long commentId, Target target,long userId, long createdTime, long destroyedTime,int feature) {
        this.ignoreId = commentId;
        this.target = target;
        this.createdTime = createdTime;
        this.destroyedTime = destroyedTime;
        this.feature = feature;
        this.userId = userId;
    }

    public long getIgnoreId() {
        return ignoreId;
    }

    public void setIgnoreId(long ignoreId) {
        this.ignoreId = ignoreId;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public long getDestroyedTime() {
        return destroyedTime;
    }

    public void setDestroyedTime(long destroyedTime) {
        this.destroyedTime = destroyedTime;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public int getFeature() {
        return feature;
    }

    public void setFeature(int feature) {
        this.feature = feature;
    }

    public void setProperty(String col, Object value) {
        Validate.notEmpty(col);
        Target target = (Target) value;
        setTarget(target);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Ignore other = (Ignore) o;
        return ignoreId == other.ignoreId
                && ObjectUtils.equals(target, other.target)
                && createdTime == other.createdTime
                && destroyedTime == other.destroyedTime
                && userId == other.userId
                && feature == other.feature;
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(ignoreId, target, createdTime, destroyedTime,feature);
    }

    @Override
    public void deserialize(JsonNode jn) {
        if (jn.has(COL_IGNORE_ID))
            setIgnoreId(jn.path(COL_IGNORE_ID).getValueAsLong());
        if (jn.has(COL_CREATED_TIME))
            setCreatedTime(jn.path(COL_CREATED_TIME).getValueAsLong());
        if (jn.has(COL_DESTROYED_TIME))
            setDestroyedTime(jn.path(COL_DESTROYED_TIME).getValueAsLong());
        if (jn.has(COL_USER))
            setUserId(jn.path(COL_USER).getValueAsLong());
        if (jn.has(COL_FEATURE))
            setFeature(jn.path(COL_FEATURE).getValueAsInt());
        if (jn.has(COL_TARGET)) {
            JsonNode objNode = jn.path(COL_TARGET);
            if (!objNode.isObject())
                throw new IllegalArgumentException("Illegal node type " + objNode.toString());
            setProperty(COL_TARGET, objNode);
        }
    }

    public void serialize(JsonGenerator jg, String[] cols) throws IOException {
        jg.writeStartObject();
        if (outputColumn(cols, COL_IGNORE_ID))
            jg.writeNumberField(COL_IGNORE_ID, getIgnoreId());
        if (outputColumn(cols, COL_DESTROYED_TIME))
            jg.writeNumberField(COL_DESTROYED_TIME, getDestroyedTime());
        if (outputColumn(cols, COL_CREATED_TIME))
            jg.writeNumberField(COL_CREATED_TIME, getCreatedTime());
        if (outputColumn(cols, COL_USER))
            jg.writeNumberField(COL_USER, getUserId());
        if (outputColumn(cols, COL_FEATURE))
            jg.writeNumberField(COL_FEATURE, getUserId());
        if (outputColumn(cols, COL_TARGET))
            jg.writeObjectField(COL_TARGET, getTarget().toCompatibleString());

        writeAddonsJson(jg, cols);
        jg.writeEndObject();
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        serialize(jg, (String[]) null);
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

    @Override
    public Ignore copy() {
        Ignore ignore = new Ignore();
        ignore.setIgnoreId(ignoreId);
        ignore.setCreatedTime(createdTime);
        ignore.setDestroyedTime(destroyedTime);
        ignore.setUserId(userId);
        ignore.setFeature(feature);
        ignore.setTarget(target);
        return ignore;
    }
}
