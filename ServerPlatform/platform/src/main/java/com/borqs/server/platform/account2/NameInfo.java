package com.borqs.server.platform.account2;


import com.borqs.server.base.data.Values;
import com.borqs.server.base.util.NameSplitter;
import com.borqs.server.platform.util.Copyable;
import com.borqs.server.platform.util.ObjectHelper;
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

public class NameInfo implements StringablePropertyBundle, JsonBean, Copyable<NameInfo> {

    public static final String COL_FIRST = "first";
    public static final String COL_MIDDLE = "middle";
    public static final String COL_LAST = "last";
    public static final String COL_FIRST_PHONETIC = "first_phonetic";
    public static final String COL_MIDDLE_PHONETIC = "middle_phonetic";
    public static final String COL_LAST_PHONETIC = "last_phonetic";
    public static final String COL_PREFIX = "prefix";
    public static final String COL_POSTFIX = "postfix";

    public static final int SUB_FIRST = 1;
    public static final int SUB_MIDDLE = 2;
    public static final int SUB_LAST = 3;
    public static final int SUB_FIRST_PHONETIC = 4;
    public static final int SUB_MIDDLE_PHONETIC = 5;
    public static final int SUB_LAST_PHONETIC = 6;
    public static final int SUB_PREFIX = 7;
    public static final int SUB_POSTFIX = 8;

    private String first = "";
    private String middle = "";
    private String last = "";
    private String firstPhonetic = "";
    private String middlePhonetic = "";
    private String lastPhonetic = "";
    private String prefix = "";
    private String postfix = "";

    public NameInfo() {
    }

    public NameInfo(String first, String last) {
        this(first, "", last);
    }

    public NameInfo(String first, String middle, String last) {
        this.first = first;
        this.middle = middle;
        this.last = last;
    }

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public String getMiddle() {
        return middle;
    }

    public void setMiddle(String middle) {
        this.middle = middle;
    }

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
    }

    public String getFirstPhonetic() {
        return firstPhonetic;
    }

    public void setFirstPhonetic(String firstPhonetic) {
        this.firstPhonetic = firstPhonetic;
    }

    public String getMiddlePhonetic() {
        return middlePhonetic;
    }

    public void setMiddlePhonetic(String middlePhonetic) {
        this.middlePhonetic = middlePhonetic;
    }

    public String getLastPhonetic() {
        return lastPhonetic;
    }

    public void setLastPhonetic(String lastPhonetic) {
        this.lastPhonetic = lastPhonetic;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPostfix() {
        return postfix;
    }

    public void setPostfix(String postfix) {
        this.postfix = postfix;
    }

    private static final TextEnum SUBMAP = TextEnum.of(new Object[][] {
            {COL_FIRST, SUB_FIRST},
            {COL_MIDDLE, SUB_MIDDLE},
            {COL_LAST, SUB_LAST},
            {COL_FIRST_PHONETIC, SUB_FIRST_PHONETIC},
            {COL_MIDDLE_PHONETIC, SUB_MIDDLE_PHONETIC},
            {COL_LAST_PHONETIC, SUB_LAST_PHONETIC},
            {COL_PREFIX, SUB_PREFIX},
            {COL_POSTFIX, SUB_POSTFIX},
    });

    @Override
    public TextEnum subMap() {
        return SUBMAP;
    }
    @Override
    public Map<Integer, Object> writeProperties(Map<Integer, Object> reuse) {
        Map<Integer, Object> m = reuse != null ? reuse : new LinkedHashMap<Integer, Object>();
        if (StringUtils.isNotEmpty(getFirst()))
            m.put(SUB_FIRST, getFirst());
        if (StringUtils.isNotEmpty(getMiddle()))
            m.put(SUB_MIDDLE, getMiddle());
        if (StringUtils.isNotEmpty(getLast()))
            m.put(SUB_LAST, getLast());
        if (StringUtils.isNotEmpty(getFirstPhonetic()))
            m.put(SUB_FIRST_PHONETIC, getFirstPhonetic());
        if (StringUtils.isNotEmpty(getMiddlePhonetic()))
            m.put(SUB_MIDDLE_PHONETIC, getMiddlePhonetic());
        if (StringUtils.isNotEmpty(getLastPhonetic()))
            m.put(SUB_LAST_PHONETIC, getLastPhonetic());
        if (StringUtils.isNotEmpty(getPrefix()))
            m.put(SUB_PREFIX, getPrefix());
        if (StringUtils.isNotEmpty(getPostfix()))
            m.put(SUB_POSTFIX, getPostfix());
        return m;
    }

    @Override
    public void readProperties(Map<Integer, Object> props, boolean partial) {
        if (!partial || props.containsKey(SUB_FIRST))
            setFirst(Values.toString(MapUtils.getObject(props, SUB_FIRST, "")));
        if (!partial || props.containsKey(SUB_MIDDLE))
            setMiddle(Values.toString(MapUtils.getObject(props, SUB_MIDDLE, "")));
        if (!partial || props.containsKey(SUB_LAST))
            setLast(Values.toString(MapUtils.getObject(props, SUB_LAST, "")));
        if (!partial || props.containsKey(SUB_FIRST_PHONETIC))
            setFirstPhonetic(Values.toString(MapUtils.getObject(props, SUB_FIRST_PHONETIC, "")));
        if (!partial || props.containsKey(SUB_MIDDLE_PHONETIC))
            setMiddlePhonetic(Values.toString(MapUtils.getObject(props, SUB_MIDDLE_PHONETIC, "")));
        if (!partial || props.containsKey(SUB_LAST_PHONETIC))
            setLastPhonetic(Values.toString(MapUtils.getObject(props, SUB_LAST_PHONETIC, "")));
        if (!partial || props.containsKey(SUB_PREFIX))
            setPrefix(Values.toString(MapUtils.getObject(props, SUB_PREFIX, "")));
        if (!partial || props.containsKey(SUB_POSTFIX))
            setPostfix(Values.toString(MapUtils.getObject(props, SUB_POSTFIX, "")));
    }

    @Override
    public void deserialize(JsonNode jn) {
        setFirst(ObjectUtils.toString(jn.path(COL_FIRST).getTextValue()));
        setMiddle(ObjectUtils.toString(jn.path(COL_MIDDLE).getTextValue()));
        setLast(ObjectUtils.toString(jn.path(COL_LAST).getTextValue()));
        setFirstPhonetic(ObjectUtils.toString(jn.path(COL_FIRST_PHONETIC).getTextValue()));
        setMiddlePhonetic(ObjectUtils.toString(jn.path(COL_MIDDLE_PHONETIC).getTextValue()));
        setLastPhonetic(ObjectUtils.toString(jn.path(COL_LAST_PHONETIC).getTextValue()));
        setPrefix(ObjectUtils.toString(jn.path(COL_PREFIX).getTextValue()));
        setPostfix(ObjectUtils.toString(jn.path(COL_POSTFIX).getTextValue()));
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        jg.writeStartObject();
        jg.writeStringField(COL_FIRST, ObjectUtils.toString(getFirst()));
        jg.writeStringField(COL_MIDDLE, ObjectUtils.toString(getMiddle()));
        jg.writeStringField(COL_LAST, ObjectUtils.toString(getLast()));
        if (StringUtils.isNotEmpty(getFirstPhonetic()))
            jg.writeStringField(COL_FIRST_PHONETIC, getFirstPhonetic());
        if (StringUtils.isNotEmpty(getMiddlePhonetic()))
            jg.writeStringField(COL_MIDDLE_PHONETIC, getMiddlePhonetic());
        if (StringUtils.isNotEmpty(getLastPhonetic()))
            jg.writeStringField(COL_LAST_PHONETIC, getLastPhonetic());
        if (StringUtils.isNotEmpty(getPrefix()))
            jg.writeStringField(COL_PREFIX, getPrefix());
        if (StringUtils.isNotEmpty(getPostfix()))
            jg.writeStringField(COL_POSTFIX, getPostfix());
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

        NameInfo other = (NameInfo) o;
        return ObjectUtils.equals(first, other.first)
                && ObjectUtils.equals(middle, other.middle)
                && ObjectUtils.equals(last, other.last)
                && ObjectUtils.equals(firstPhonetic, other.firstPhonetic)
                && ObjectUtils.equals(middlePhonetic, other.middlePhonetic)
                && ObjectUtils.equals(lastPhonetic, other.lastPhonetic)
                && ObjectUtils.equals(prefix, other.prefix)
                && ObjectUtils.equals(postfix, other.postfix);
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(first, middle, last,
                firstPhonetic, middlePhonetic, lastPhonetic, prefix, postfix);
    }

    @Override
    public NameInfo copy() {
        NameInfo info = new NameInfo();
        info.setFirst(first);
        info.setMiddle(middle);
        info.setLast(last);
        info.setFirstPhonetic(firstPhonetic);
        info.setMiddlePhonetic(middlePhonetic);
        info.setLastPhonetic(lastPhonetic);
        info.setPrefix(prefix);
        info.setPostfix(postfix);
        return info;
    }

    public String getDisplayName() {
        NameSplitter.Name n = new NameSplitter.Name(prefix, first, middle, last, postfix);
        return NameSplitter.join(n);
    }

    public static NameInfo split(String fullName) {
        NameSplitter.Name n = NameSplitter.split(fullName);
        NameInfo info = new NameInfo();
        info.setFirst(ObjectUtils.toString(n.getGivenNames()));
        info.setMiddle(ObjectUtils.toString(n.getMiddleName()));
        info.setLast(ObjectUtils.toString(n.getFamilyName()));
        info.setFirstPhonetic(ObjectUtils.toString(n.getPhoneticGivenName()));
        info.setMiddlePhonetic(ObjectUtils.toString(n.getPhoneticMiddleName()));
        info.setLastPhonetic(ObjectUtils.toString(n.getPhoneticFamilyName()));
        info.setPrefix(ObjectUtils.toString(n.getPrefix()));
        info.setPostfix(ObjectUtils.toString(n.getSuffix()));
        return info;
    }
}
