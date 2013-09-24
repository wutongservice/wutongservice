package com.borqs.server.wutong.account2.user;


import com.borqs.server.base.data.Values;
import com.borqs.server.wutong.account2.util.ObjectHelper;
import com.borqs.server.wutong.account2.util.TextEnum;
import com.borqs.server.wutong.account2.util.json.JsonBean;
import com.borqs.server.wutong.account2.util.json.JsonHelper;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class TypedInfo implements StringablePropertyBundle, JsonBean {

    public static final String COL_TYPE = "type";
    public static final String COL_INFO = "info";
    public static final String COL_PRIMARY = "primary";
    public static final String COL_LABEL = "label";

    public static final int SUB_TYPE = 1;
    public static final int SUB_INFO = 2;
    public static final int SUB_FLAG = 3;
    public static final int SUB_LABEL = 4;

    public static final int FLAG_PRIMARY = 1;

    protected String type = "";
    protected String info = "";
    protected int flag = 0;
    protected String label = "";

    protected TypedInfo() {
    }

    protected TypedInfo(String type, String info) {
        this(type, info, false, "");
    }

    protected TypedInfo(String type, String info, boolean primary, String label) {
        this.type = type;
        this.info = info;
        setPrimary(primary);
        this.label = label;
    }

    protected TypedInfo assignFields(String type, String info, int flag, String label) {
        this.type = type;
        this.info = info;
        this.flag = flag;
        this.label = label;
        return this;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        Validate.isTrue(type != null && checkType(type), "Type error", type);
        this.type = type;
    }

    protected abstract boolean checkType(String type);

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public boolean isPrimary() {
        return (flag & FLAG_PRIMARY) != 0;
    }

    public void setPrimary(boolean primary) {
        if (primary)
            flag |= FLAG_PRIMARY;
        else
            flag &= ~FLAG_PRIMARY;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    private static final TextEnum SUBMAP = TextEnum.of(new Object[][] {
            {COL_TYPE, SUB_TYPE},
            {COL_INFO, SUB_INFO},
    });

    @Override
    public TextEnum subMap() {
        return SUBMAP;
    }

    @Override
    public Map<Integer, Object> writeProperties(Map<Integer, Object> reuse) {
        Map<Integer, Object> m = reuse != null ? reuse : new LinkedHashMap<Integer, Object>();
        m.put(SUB_TYPE, getType());
        m.put(SUB_INFO, getInfo());
        if (flag != 0)
            m.put(SUB_FLAG, flag);
        if (StringUtils.isNotEmpty(getLabel()))
            m.put(SUB_LABEL, getLabel());
        return m;
    }

    @Override
    public void readProperties(Map<Integer, Object> props, boolean partial) {
        if (!partial || props.containsKey(SUB_TYPE))
            setType(Values.toString(MapUtils.getObject(props, SUB_TYPE, "")));
        if (!partial || props.containsKey(SUB_INFO))
            setInfo(Values.toString(MapUtils.getObject(props, SUB_INFO, "")));
        if (!partial || props.containsKey(SUB_INFO))
            flag = (int) Values.toInt(MapUtils.getObject(props, SUB_FLAG, "0"));
        if (!partial || props.containsKey(SUB_LABEL))
            setLabel(Values.toString(MapUtils.getObject(props, SUB_LABEL, "")));
    }

    @Override
    public void deserialize(JsonNode jn) {
        setType(ObjectUtils.toString(jn.path(COL_TYPE).getTextValue(), ""));
        setInfo(ObjectUtils.toString(jn.path(COL_INFO).getTextValue(), ""));
        setPrimary(jn.path(COL_PRIMARY).getBooleanValue());
        setLabel(ObjectUtils.toString(jn.path(COL_LABEL).getTextValue(), ""));
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        jg.writeStartObject();
        jg.writeStringField(COL_TYPE, getType());
        jg.writeStringField(COL_INFO, getInfo());
        if (isPrimary())
            jg.writeBooleanField(COL_PRIMARY, isPrimary());
        if (StringUtils.isNotEmpty(getLabel()))
            jg.writeStringField(COL_LABEL, getLabel());
        jg.writeEndObject();
    }

    @Override
    public String toString() {
        return JsonHelper.toJson(this, true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        TypedInfo other = (TypedInfo) o;
        return ObjectUtils.equals(type, other.type)
                && ObjectUtils.equals(info, other.info)
                && ObjectUtils.equals(flag, other.flag)
                && ObjectUtils.equals(label, other.label);
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(type, info, flag, label);
    }
}
