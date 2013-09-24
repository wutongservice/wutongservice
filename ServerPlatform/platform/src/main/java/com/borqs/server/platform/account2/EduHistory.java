package com.borqs.server.platform.account2;


import com.borqs.server.base.data.Values;
import com.borqs.server.platform.util.Copyable;
import com.borqs.server.platform.util.ObjectHelper;
import com.borqs.server.platform.util.TextEnum;
import com.borqs.server.platform.util.json.JsonBean;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
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

public class EduHistory extends HistoryInfo implements StringablePropertyBundle, JsonBean, Copyable<EduHistory> {

    public static final String COL_TYPE = "type";
    public static final String COL_SCHOOL = "school";
    public static final String COL_CLASS = "class";
    public static final String COL_DEGREE = "degree";
    public static final String COL_MAJOR = "major";
    public static final String COL_LABEL = "label";

    public static final int SUB_FROM = 1;
    public static final int SUB_TO = 2;
    public static final int SUB_TYPE = 3;
    public static final int SUB_SCHOOL = 4;
    public static final int SUB_CLASS = 5;
    public static final int SUB_DEGREE = 6;
    public static final int SUB_MAJOR = 7;
    public static final int SUB_LABEL = 8;

    public static final String TYPE_PRIMARY_SCHOOL = "primary_school";
    public static final String TYPE_JUNIOR_MIDDLE_SCHOOL = "junior_middle_school";
    public static final String TYPE_SENIOR_MIDDLE_SCHOOL = "senior_middle_school";
    public static final String TYPE_UNIVERSITY = "university";
    public static final String[] TYPES = {
            TYPE_PRIMARY_SCHOOL,
            TYPE_JUNIOR_MIDDLE_SCHOOL,
            TYPE_SENIOR_MIDDLE_SCHOOL,
            TYPE_UNIVERSITY,
    };

    private String type = "";
    private String school = "";
    private String klass = "";
    private String degree = "";
    private String major = "";
    private String label = "";

    public EduHistory() {
    }

    public EduHistory(String from, String to, String type, String school, String klass, String degree, String major, String label) {
        super(from, to);
        this.type = type;
        this.school = school;
        this.klass = klass;
        this.degree = degree;
        this.major = major;
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        Validate.isTrue(type != null && checkType(type), "Type error", type);
        this.type = type;
    }

    protected boolean checkType(String type) {
        return type.isEmpty() || ArrayUtils.contains(TYPES, type) || type.startsWith("x-");
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getKlass() {
        return klass;
    }

    public void setKlass(String klass) {
        this.klass = klass;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    private static final TextEnum SUBMAP = TextEnum.of(new Object[][] {
            {COL_FROM, SUB_FROM},
            {COL_TO, SUB_TO},
            {COL_TYPE, SUB_TYPE},
            {COL_SCHOOL, SUB_SCHOOL},
            {COL_CLASS, SUB_CLASS},
            {COL_DEGREE, SUB_DEGREE},
            {COL_MAJOR, SUB_MAJOR},
            {COL_LABEL, SUB_LABEL},
    });

    @Override
    public TextEnum subMap() {
        return SUBMAP;
    }
    @Override
    public Map<Integer, Object> writeProperties(Map<Integer, Object> reuse) {
        Map<Integer, Object> m = reuse != null ? reuse : new LinkedHashMap<Integer, Object>();
        if (StringUtils.isNotEmpty(getFrom()))
            m.put(SUB_FROM, getFrom());
        if (StringUtils.isNotEmpty(getTo()))
            m.put(SUB_TO, getTo());
        if (StringUtils.isNotEmpty(getType()))
            m.put(SUB_TYPE, getType());
        if (StringUtils.isNotEmpty(getSchool()))
            m.put(SUB_SCHOOL, getSchool());
        if (StringUtils.isNotEmpty(getKlass()))
            m.put(SUB_CLASS, getKlass());
        if (StringUtils.isNotEmpty(getDegree()))
            m.put(SUB_DEGREE, getDegree());
        if (StringUtils.isNotEmpty(getMajor()))
            m.put(SUB_MAJOR, getMajor());
        if (StringUtils.isNotEmpty(getLabel()))
            m.put(SUB_LABEL, getLabel());
        return m;
    }

    @Override
    public void readProperties(Map<Integer, Object> props, boolean partial) {
        if (!partial || props.containsKey(SUB_FROM))
            setFrom(Values.toString(MapUtils.getObject(props, SUB_FROM, "")));
        if (!partial || props.containsKey(SUB_TO))
            setTo(Values.toString(MapUtils.getObject(props, SUB_TO, "")));
        if (!partial || props.containsKey(SUB_TYPE))
            setType(Values.toString(MapUtils.getObject(props, SUB_TYPE, "")));
        if (!partial || props.containsKey(SUB_SCHOOL))
            setSchool(Values.toString(MapUtils.getObject(props, SUB_SCHOOL, "")));
        if (!partial || props.containsKey(SUB_CLASS))
            setKlass(Values.toString(MapUtils.getObject(props, SUB_CLASS, "")));
        if (!partial || props.containsKey(SUB_DEGREE))
            setDegree(Values.toString(MapUtils.getObject(props, SUB_DEGREE, "")));
        if (!partial || props.containsKey(SUB_MAJOR))
            setMajor(Values.toString(MapUtils.getObject(props, SUB_MAJOR, "")));
        if (!partial || props.containsKey(SUB_LABEL))
            setLabel(Values.toString(MapUtils.getObject(props, SUB_LABEL, "")));
    }

    @Override
    public void deserialize(JsonNode jn) {
        setFrom(ObjectUtils.toString(jn.path(COL_FROM).getTextValue()));
        setTo(ObjectUtils.toString(jn.path(COL_TO).getTextValue()));
        setType(ObjectUtils.toString(jn.path(COL_TYPE).getTextValue()));
        setSchool(ObjectUtils.toString(jn.path(COL_SCHOOL).getTextValue()));
        setKlass(ObjectUtils.toString(jn.path(COL_CLASS).getTextValue()));
        setDegree(ObjectUtils.toString(jn.path(COL_DEGREE).getTextValue()));
        setMajor(ObjectUtils.toString(jn.path(COL_MAJOR).getTextValue()));
        setLabel(ObjectUtils.toString(jn.path(COL_LABEL).getTextValue()));
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        jg.writeStartObject();
        jg.writeStringField(COL_FROM, ObjectUtils.toString(getFrom()));
        jg.writeStringField(COL_TO, ObjectUtils.toString(getTo()));
        jg.writeStringField(COL_TYPE, ObjectUtils.toString(getType()));
        jg.writeStringField(COL_SCHOOL, ObjectUtils.toString(getSchool()));
        jg.writeStringField(COL_CLASS, ObjectUtils.toString(getKlass()));
        jg.writeStringField(COL_DEGREE, ObjectUtils.toString(getDegree()));
        jg.writeStringField(COL_MAJOR, ObjectUtils.toString(getMajor()));
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

        EduHistory other = (EduHistory) o;
        return StringUtils.equals(from, other.from)
                && StringUtils.equals(to, other.to)
                && StringUtils.equals(type, other.type)
                && StringUtils.equals(school, other.school)
                && StringUtils.equals(klass, other.klass)
                && StringUtils.equals(degree, other.degree)
                && StringUtils.equals(major, other.major)
                && StringUtils.equals(label, other.label);
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(from, to, type, school, klass, degree, major, label);
    }

    @Override
    public EduHistory copy() {
        EduHistory info = new EduHistory();
        info.setFrom(from);
        info.setTo(to);
        info.setType(type);
        info.setSchool(school);
        info.setKlass(klass);
        info.setDegree(degree);
        info.setMajor(major);
        info.setLabel(label);
        return info;
    }
}
