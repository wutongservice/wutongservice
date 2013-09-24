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

public class OrgInfo implements StringablePropertyBundle, JsonBean, Copyable<OrgInfo> {

    public static final String COL_TYPE = "type";
    public static final String COL_COMPANY = "company";
    public static final String COL_TITLE = "title";
    public static final String COL_DEPARTMENT = "department";
    public static final String COL_OFFICE_LOCATION = "office_location";
    public static final String COL_JOB_DESCRIPTION = "job_description";
    public static final String COL_SYMBOL = "symbol";
    public static final String COL_PHONETIC_NAME = "phonetic_name";
    public static final String COL_LABEL = "label";

    public static final int SUB_TYPE = 1;
    public static final int SUB_COMPANY = 2;
    public static final int SUB_TITLE = 3;
    public static final int SUB_DEPARTMENT = 4;
    public static final int SUB_OFFICE_LOCATION = 5;
    public static final int SUB_JOB_DESCRIPTION = 6;
    public static final int SUB_SYMBOL = 7;
    public static final int SUB_PHONETIC_NAME = 8;
    public static final int SUB_LABEL = 9;

    public static final String TYPE_WORK = "work";
    public static final String TYPE_OTHER = "other";
    public static final String[] TYPES = {TYPE_WORK, TYPE_OTHER};

    private String type = "";
    private String company = "";
    private String title = "";
    private String department = "";
    private String officeLocation = "";
    private String jobDescription = "";
    private String symbol = "";
    private String phoneticName = "";
    private String label = "";

    public OrgInfo() {
    }

    public OrgInfo(String type,
                   String company,
                   String title,
                   String department,
                   String officeLocation,
                   String jobDescription,
                   String symbol,
                   String phoneticName,
                   String label) {
        this.type = type;
        this.company = company;
        this.title = title;
        this.department = department;
        this.officeLocation = officeLocation;
        this.jobDescription = jobDescription;
        this.symbol = symbol;
        this.phoneticName = phoneticName;
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

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getOfficeLocation() {
        return officeLocation;
    }

    public void setOfficeLocation(String officeLocation) {
        this.officeLocation = officeLocation;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getPhoneticName() {
        return phoneticName;
    }

    public void setPhoneticName(String phoneticName) {
        this.phoneticName = phoneticName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    private static final TextEnum SUBMAP = TextEnum.of(new Object[][] {
            {COL_TYPE, SUB_TYPE},
            {COL_COMPANY, SUB_COMPANY},
            {COL_TITLE, SUB_TITLE},
            {COL_DEPARTMENT, SUB_DEPARTMENT},
            {COL_OFFICE_LOCATION, SUB_OFFICE_LOCATION},
            {COL_JOB_DESCRIPTION, SUB_JOB_DESCRIPTION},
            {COL_SYMBOL, SUB_SYMBOL},
            {COL_PHONETIC_NAME, SUB_PHONETIC_NAME},
            {COL_LABEL, SUB_LABEL},
    });

    @Override
    public TextEnum subMap() {
        return SUBMAP;
    }
    @Override
    public Map<Integer, Object> writeProperties(Map<Integer, Object> reuse) {
        Map<Integer, Object> m = reuse != null ? reuse : new LinkedHashMap<Integer, Object>();
        if (StringUtils.isNotEmpty(getType()))
            m.put(SUB_TYPE, getType());
        if (StringUtils.isNotEmpty(getCompany()))
            m.put(SUB_COMPANY, getCompany());
        if (StringUtils.isNotEmpty(getTitle()))
            m.put(SUB_TITLE, getTitle());
        if (StringUtils.isNotEmpty(getDepartment()))
            m.put(SUB_DEPARTMENT, getDepartment());
        if (StringUtils.isNotEmpty(getOfficeLocation()))
            m.put(SUB_OFFICE_LOCATION, getOfficeLocation());
        if (StringUtils.isNotEmpty(getJobDescription()))
            m.put(SUB_JOB_DESCRIPTION, getJobDescription());
        if (StringUtils.isNotEmpty(getSymbol()))
            m.put(SUB_SYMBOL, getSymbol());
        if (StringUtils.isNotEmpty(getPhoneticName()))
            m.put(SUB_PHONETIC_NAME, getPhoneticName());
        if (StringUtils.isNotEmpty(getLabel()))
            m.put(SUB_LABEL, getLabel());
        return m;
    }

    @Override
    public void readProperties(Map<Integer, Object> props, boolean partial) {
        if (!partial || props.containsKey(SUB_TYPE))
            setType(Values.toString(MapUtils.getObject(props, SUB_TYPE, "")));
        if (!partial || props.containsKey(SUB_COMPANY))
            setCompany(Values.toString(MapUtils.getObject(props, SUB_COMPANY, "")));
        if (!partial || props.containsKey(SUB_TITLE))
            setTitle(Values.toString(MapUtils.getObject(props, SUB_TITLE, "")));
        if (!partial || props.containsKey(SUB_DEPARTMENT))
            setDepartment(Values.toString(MapUtils.getObject(props, SUB_DEPARTMENT, "")));
        if (!partial || props.containsKey(SUB_OFFICE_LOCATION))
            setOfficeLocation(Values.toString(MapUtils.getObject(props, SUB_OFFICE_LOCATION, "")));
        if (!partial || props.containsKey(SUB_JOB_DESCRIPTION))
            setJobDescription(Values.toString(MapUtils.getObject(props, SUB_JOB_DESCRIPTION, "")));
        if (!partial || props.containsKey(SUB_SYMBOL))
            setSymbol(Values.toString(MapUtils.getObject(props, SUB_SYMBOL, "")));
        if (!partial || props.containsKey(SUB_PHONETIC_NAME))
            setPhoneticName(Values.toString(MapUtils.getObject(props, SUB_PHONETIC_NAME, "")));
        if (!partial || props.containsKey(SUB_LABEL))
            setLabel(Values.toString(MapUtils.getObject(props, SUB_LABEL, "")));
    }

    @Override
    public void deserialize(JsonNode jn) {
        setType(ObjectUtils.toString(jn.path(COL_TYPE).getTextValue()));
        setCompany(ObjectUtils.toString(jn.path(COL_COMPANY).getTextValue()));
        setTitle(ObjectUtils.toString(jn.path(COL_TITLE).getTextValue()));
        setDepartment(ObjectUtils.toString(jn.path(COL_DEPARTMENT).getTextValue()));
        setOfficeLocation(ObjectUtils.toString(jn.path(COL_OFFICE_LOCATION).getTextValue()));
        setJobDescription(ObjectUtils.toString(jn.path(COL_JOB_DESCRIPTION).getTextValue()));
        setSymbol(ObjectUtils.toString(jn.path(COL_SYMBOL).getTextValue()));
        setPhoneticName(ObjectUtils.toString(jn.path(COL_PHONETIC_NAME).getTextValue()));
        setLabel(ObjectUtils.toString(jn.path(COL_LABEL).getTextValue()));
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        jg.writeStartObject();
        jg.writeStringField(COL_TYPE, ObjectUtils.toString(getType()));
        jg.writeStringField(COL_COMPANY, ObjectUtils.toString(getCompany()));
        jg.writeStringField(COL_TITLE, ObjectUtils.toString(getTitle()));
        jg.writeStringField(COL_DEPARTMENT, ObjectUtils.toString(getDepartment()));
        jg.writeStringField(COL_OFFICE_LOCATION, ObjectUtils.toString(getOfficeLocation()));
        jg.writeStringField(COL_JOB_DESCRIPTION, ObjectUtils.toString(getJobDescription()));
        jg.writeStringField(COL_SYMBOL, ObjectUtils.toString(getSymbol()));
        if (StringUtils.isNotEmpty(getPhoneticName()))
            jg.writeStringField(COL_PHONETIC_NAME, getPhoneticName());
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

        OrgInfo other = (OrgInfo) o;
        return ObjectUtils.equals(type, other.type)
                && ObjectUtils.equals(company, other.company)
                && ObjectUtils.equals(title, other.title)
                && ObjectUtils.equals(department, other.department)
                && ObjectUtils.equals(officeLocation, other.officeLocation)
                && ObjectUtils.equals(jobDescription, other.jobDescription)
                && ObjectUtils.equals(symbol, other.symbol)
                && ObjectUtils.equals(phoneticName, other.phoneticName)
                && ObjectUtils.equals(label, other.label);
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(type, company, title, department, officeLocation, jobDescription, symbol, phoneticName, label);
    }

    @Override
    public OrgInfo copy() {
        OrgInfo info = new OrgInfo();
        info.setType(type);
        info.setCompany(company);
        info.setTitle(title);
        info.setDepartment(department);
        info.setOfficeLocation(officeLocation);
        info.setJobDescription(jobDescription);
        info.setSymbol(symbol);
        info.setPhoneticName(phoneticName);
        info.setLabel(label);
        return info;
    }
}
