package com.borqs.server.platform.feature.account;


import com.borqs.server.platform.data.Values;
import com.borqs.server.platform.util.Copyable;
import com.borqs.server.platform.util.ObjectHelper;
import com.borqs.server.platform.util.TextEnum;
import com.borqs.server.platform.util.json.JsonBean;
import com.borqs.server.platform.util.json.JsonHelper;
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

public class ProfileInfo implements StringablePropertyBundle, JsonBean, Copyable<ProfileInfo> {

    public static final String COL_GENDER = "gender";
    public static final String COL_TIMEZONE = "timezone";
    public static final String COL_INTERESTS = "interests";
    public static final String COL_LANGUAGES = "languages";
    public static final String COL_MARRIAGE = "marriage";
    public static final String COL_RELIGION = "religion";
    public static final String COL_DESCRIPTION = "description";

    public static final int SUB_GENDER = 1;
    public static final int SUB_TIMEZONE = 2;
    public static final int SUB_INTERESTS = 3;
    public static final int SUB_LANGUAGES = 4;
    public static final int SUB_MARRIAGE = 5;
    public static final int SUB_RELIGION = 6;
    public static final int SUB_DESCRIPTION = 7;

    public static final String GENDER_MALE = "m";
    public static final String GENDER_FEMALE = "f";
    public static final String GENDER_UNDEFINED = "u";

    public static final String MARRIAGE_MARRIED = "y";
    public static final String MARRIAGE_SPINSTERHOOD = "n";
    public static final String MARRIAGE_UNDEFINED = "u";

    private String gender = GENDER_UNDEFINED;
    private String timezone = "";
    private String interests = "";
    private String languages = "";
    private String marriage = MARRIAGE_UNDEFINED;
    private String religion = "";
    private String description = "";

    public ProfileInfo() {
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        Validate.isTrue(GENDER_MALE.equals(gender) || GENDER_FEMALE.equals(gender) || GENDER_UNDEFINED.equals(gender));
        this.gender = gender;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public String getLanguages() {
        return languages;
    }

    public void setLanguages(String languages) {
        this.languages = languages;
    }

    public String getMarriage() {
        return marriage;
    }

    public void setMarriage(String marriage) {
        Validate.isTrue(MARRIAGE_MARRIED.equals(marriage) || MARRIAGE_SPINSTERHOOD.equals(marriage) || MARRIAGE_UNDEFINED.equals(marriage));
        this.marriage = marriage;
    }

    public String getReligion() {
        return religion;
    }

    public void setReligion(String religion) {
        this.religion = religion;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private static final TextEnum SUBMAP = TextEnum.of(new Object[][] {
            {COL_GENDER, SUB_GENDER},
            {COL_TIMEZONE, SUB_TIMEZONE},
            {COL_INTERESTS, SUB_INTERESTS},
            {COL_LANGUAGES, SUB_LANGUAGES},
            {COL_MARRIAGE, SUB_MARRIAGE},
            {COL_RELIGION, SUB_RELIGION},
            {COL_DESCRIPTION, SUB_DESCRIPTION},
    });

    @Override
    public TextEnum subMap() {
        return SUBMAP;
    }

    @Override
    public Map<Integer, Object> writeProperties(Map<Integer, Object> reuse) {
        Map<Integer, Object> m = reuse != null ? reuse : new LinkedHashMap<Integer, Object>();
        if (StringUtils.isNotEmpty(getGender()))
            m.put(SUB_GENDER, getGender());
        if (StringUtils.isNotEmpty(getTimezone()))
            m.put(SUB_TIMEZONE, getTimezone());
        if (StringUtils.isNotEmpty(getInterests()))
            m.put(SUB_INTERESTS, getInterests());
        if (StringUtils.isNotEmpty(getLanguages()))
            m.put(SUB_LANGUAGES, getLanguages());
        if (StringUtils.isNotEmpty(getMarriage()))
            m.put(SUB_MARRIAGE, getMarriage());
        if (StringUtils.isNotEmpty(getReligion()))
            m.put(SUB_RELIGION, getReligion());
        if (StringUtils.isNotEmpty(getDescription()))
            m.put(SUB_DESCRIPTION, getDescription());
        return m;
    }

    @Override
    public void readProperties(Map<Integer, Object> props, boolean partial) {
        if (!partial || props.containsKey(SUB_GENDER))
            setGender(Values.toString(MapUtils.getObject(props, SUB_GENDER, "u")));
        if (!partial || props.containsKey(SUB_TIMEZONE))
            setTimezone(Values.toString(MapUtils.getObject(props, SUB_TIMEZONE, "")));
        if (!partial || props.containsKey(SUB_INTERESTS))
            setInterests(Values.toString(MapUtils.getObject(props, SUB_INTERESTS, "")));
        if (!partial || props.containsKey(SUB_LANGUAGES))
            setLanguages(Values.toString(MapUtils.getObject(props, SUB_LANGUAGES, "")));
        if (!partial || props.containsKey(SUB_MARRIAGE))
            setMarriage(Values.toString(MapUtils.getObject(props, SUB_MARRIAGE, "u")));
        if (!partial || props.containsKey(SUB_RELIGION))
            setReligion(Values.toString(MapUtils.getObject(props, SUB_RELIGION, "")));
        if (!partial || props.containsKey(SUB_DESCRIPTION))
            setDescription(Values.toString(MapUtils.getObject(props, SUB_DESCRIPTION, "")));
    }

    @Override
    public void deserialize(JsonNode jn) {
        setGender(ObjectUtils.toString(jn.path(COL_GENDER).getTextValue()));
        setTimezone(ObjectUtils.toString(jn.path(COL_TIMEZONE).getTextValue()));
        setInterests(ObjectUtils.toString(jn.path(COL_INTERESTS).getTextValue()));
        setLanguages(ObjectUtils.toString(jn.path(COL_LANGUAGES).getTextValue()));
        setMarriage(ObjectUtils.toString(jn.path(COL_MARRIAGE).getTextValue()));
        setReligion(ObjectUtils.toString(jn.path(COL_RELIGION).getTextValue()));
        setDescription(ObjectUtils.toString(jn.path(COL_DESCRIPTION).getTextValue()));
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        jg.writeStartObject();
        jg.writeStringField(COL_GENDER, ObjectUtils.toString(getGender()));
        jg.writeStringField(COL_TIMEZONE, ObjectUtils.toString(getTimezone()));
        jg.writeStringField(COL_INTERESTS, ObjectUtils.toString(getInterests()));
        jg.writeStringField(COL_LANGUAGES, ObjectUtils.toString(getLanguages()));
        jg.writeStringField(COL_MARRIAGE, ObjectUtils.toString(getMarriage()));
        jg.writeStringField(COL_RELIGION, ObjectUtils.toString(getReligion()));
        jg.writeStringField(COL_DESCRIPTION, ObjectUtils.toString(getDescription()));
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

        ProfileInfo other = (ProfileInfo) o;
        return ObjectUtils.equals(gender, other.gender)
                && ObjectUtils.equals(timezone, other.timezone)
                && ObjectUtils.equals(interests, other.interests)
                && ObjectUtils.equals(languages, other.languages)
                && ObjectUtils.equals(marriage, other.marriage)
                && ObjectUtils.equals(religion, other.religion)
                && ObjectUtils.equals(description, other.description);
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(gender, timezone, interests, languages, marriage, religion, description);
    }

    @Override
    public ProfileInfo copy() {
        ProfileInfo info = new ProfileInfo();
        info.setGender(gender);
        info.setTimezone(timezone);
        info.setInterests(interests);
        info.setLanguages(languages);
        info.setMarriage(marriage);
        info.setReligion(religion);
        info.setDescription(description);
        return info;
    }
}
