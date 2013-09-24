package com.borqs.server.platform.feature.account;


import com.borqs.server.platform.data.Values;
import com.borqs.server.platform.util.Copyable;
import com.borqs.server.platform.util.ObjectHelper;
import com.borqs.server.platform.util.StringHelper;
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

public class PhotoInfo implements StringablePropertyBundle, JsonBean, Copyable<PhotoInfo> {

    public static final String COL_MIDDLE_URL = "middle_url";
    public static final String COL_SMALL_URL = "small_url";
    public static final String COL_LARGE_URL = "large_url";

    public static final int SUB_MIDDLE_URL = 1;
    public static final int SUB_SMALL_URL = 2;
    public static final int SUB_LARGE_URL = 3;

    private String middleUrl = "";
    private String smallUrl = "";
    private String largeUrl = "";

    public PhotoInfo() {
    }

    public PhotoInfo(String middleUrl, String smallUrl, String largeUrl) {
        this.middleUrl = middleUrl;
        this.smallUrl = smallUrl;
        this.largeUrl = largeUrl;
    }

    public String getMiddleUrl() {
        return middleUrl;
    }

    public void setMiddleUrl(String middleUrl) {
        this.middleUrl = middleUrl;
    }

    public String getSmallUrl() {
        return smallUrl;
    }

    public void setSmallUrl(String smallUrl) {
        this.smallUrl = smallUrl;
    }

    public String getLargeUrl() {
        return largeUrl;
    }

    public void setLargeUrl(String largeUrl) {
        this.largeUrl = largeUrl;
    }

    public void addUrlPrefix(String prefix) {
        prefix = StringUtils.trimToEmpty(prefix);
        if (!prefix.endsWith("/"))
            prefix += "/";

        middleUrl = StringHelper.addPrefix(middleUrl, prefix, true);
        smallUrl = StringHelper.addPrefix(smallUrl, prefix, true);
        largeUrl = StringHelper.addPrefix(largeUrl, prefix, true);
    }

    private static final TextEnum SUBMAP = TextEnum.of(new Object[][] {
            {COL_MIDDLE_URL, SUB_MIDDLE_URL},
            {COL_SMALL_URL, SUB_SMALL_URL},
            {COL_LARGE_URL, SUB_LARGE_URL},
    });

    @Override
    public TextEnum subMap() {
        return SUBMAP;
    }

    @Override
    public Map<Integer, Object> writeProperties(Map<Integer, Object> reuse) {
        Map<Integer, Object> m = reuse != null ? reuse : new LinkedHashMap<Integer, Object>();
        m.put(SUB_MIDDLE_URL, getMiddleUrl());
        m.put(SUB_SMALL_URL, getSmallUrl());
        m.put(SUB_LARGE_URL, getLargeUrl());
        return m;
    }

    @Override
    public void readProperties(Map<Integer, Object> props, boolean partial) {
        if (!partial || props.containsKey(SUB_MIDDLE_URL))
            setMiddleUrl(Values.toString(MapUtils.getObject(props, SUB_MIDDLE_URL, "")));
        if (!partial || props.containsKey(SUB_SMALL_URL))
            setSmallUrl(Values.toString(MapUtils.getObject(props, SUB_SMALL_URL, "")));
        if (!partial || props.containsKey(SUB_LARGE_URL))
            setLargeUrl(Values.toString(MapUtils.getObject(props, SUB_LARGE_URL, "")));
    }

    @Override
    public void deserialize(JsonNode jn) {
        setMiddleUrl(ObjectUtils.toString(jn.path(COL_MIDDLE_URL).getTextValue()));
        setSmallUrl(ObjectUtils.toString(jn.path(COL_SMALL_URL).getTextValue()));
        setLargeUrl(ObjectUtils.toString(jn.path(COL_LARGE_URL).getTextValue()));
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        jg.writeStartObject();
        jg.writeStringField(COL_MIDDLE_URL, ObjectUtils.toString(getMiddleUrl()));
        jg.writeStringField(COL_SMALL_URL, ObjectUtils.toString(getSmallUrl()));
        jg.writeStringField(COL_LARGE_URL, ObjectUtils.toString(getLargeUrl()));
        jg.writeEndObject();
    }

    @Override
    public String toString() {
        return JsonHelper.toJson(this, true);
    }

    @Override
    public PhotoInfo copy() {
        return new PhotoInfo(middleUrl, smallUrl, largeUrl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        PhotoInfo other = (PhotoInfo) o;
        return ObjectUtils.equals(middleUrl, other.middleUrl)
                && ObjectUtils.equals(smallUrl, other.smallUrl)
                && ObjectUtils.equals(largeUrl, other.largeUrl);
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(middleUrl, smallUrl, largeUrl);
    }
}
