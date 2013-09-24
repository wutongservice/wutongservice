package com.borqs.server.platform.account2;


import com.borqs.server.base.data.Values;
import com.borqs.server.platform.util.Copyable;
import com.borqs.server.platform.util.TextEnum;
import com.borqs.server.platform.util.json.JsonBean;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MiscInfo implements StringablePropertyBundle, JsonBean, Copyable<MiscInfo> {

    public static final String COL_OPENFACE_PHONE = "openface.phone";

    public static final int SUB_OPENFACE_PHONE = 1;

    private String openfacePhone = "0";

    public MiscInfo() {
    }

    public String getOpenfacePhone() {
        return openfacePhone;
    }

    public void setOpenfacePhone(String openfacePhone) {
        this.openfacePhone = openfacePhone;
    }

    private static final TextEnum SUBMAP = TextEnum.of(new Object[][] {
            {COL_OPENFACE_PHONE, SUB_OPENFACE_PHONE},
    });

    @Override
    public TextEnum subMap() {
        return SUBMAP;
    }
    @Override
    public Map<Integer, Object> writeProperties(Map<Integer, Object> reuse) {
        Map<Integer, Object> m = reuse != null ? reuse : new LinkedHashMap<Integer, Object>();
        if (StringUtils.isNotEmpty(getOpenfacePhone()))
            m.put(SUB_OPENFACE_PHONE, getOpenfacePhone());
        return m;
    }

    @Override
    public void readProperties(Map<Integer, Object> props, boolean partial) {
        if (!partial || props.containsKey(SUB_OPENFACE_PHONE))
            setOpenfacePhone(Values.toString(MapUtils.getObject(props, SUB_OPENFACE_PHONE, "0")));
    }

    @Override
    public void deserialize(JsonNode jn) {
        setOpenfacePhone(ObjectUtils.toString(jn.path(COL_OPENFACE_PHONE).getTextValue(), "0"));
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        jg.writeStartObject();
        jg.writeStringField(COL_OPENFACE_PHONE, ObjectUtils.toString(getOpenfacePhone()));
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

        MiscInfo other = (MiscInfo) o;
        return StringUtils.equals(openfacePhone, other.openfacePhone);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hashCode(openfacePhone);
    }

    @Override
    public MiscInfo copy() {
        MiscInfo info = new MiscInfo();
        info.setOpenfacePhone(openfacePhone);
        return info;
    }
}
