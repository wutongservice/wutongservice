package com.borqs.server.platform.account2;


import com.borqs.server.base.data.Values;
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

public class WorkHistory extends HistoryInfo implements StringablePropertyBundle, JsonBean, Copyable<WorkHistory> {

    public static final String COL_COMPANY = "company";
    public static final String COL_TITLE = "title";
    public static final String COL_DEPARTMENT = "department";
    public static final String COL_OFFICE_LOCATION = "office_location";
    public static final String COL_JOB_DESCRIPTION = "job_description";
    public static final String COL_SYMBOL = "symbol";
    public static final String COL_PHONETIC_NAME = "phonetic_name";

    public static final int SUB_FROM = 1;
    public static final int SUB_TO = 2;
    public static final int SUB_COMPANY = 3;
    public static final int SUB_TITLE = 4;
    public static final int SUB_DEPARTMENT = 5;
    public static final int SUB_OFFICE_LOCATION = 6;
    public static final int SUB_JOB_DESCRIPTION = 7;
    public static final int SUB_SYMBOL = 8;
    public static final int SUB_PHONETIC_NAME = 9;

    private String company = "";
    private String title = "";
    private String department = "";
    private String officeLocation = "";
    private String jobDescription = "";
    private String symbol = "";
    private String phoneticName = "";

    public WorkHistory() {
    }

    public WorkHistory(String from, String to, String company, String title, String department, String officeLocation, String jobDescription, String symbol, String phoneticName) {
        super(from, to);
        this.company = company;
        this.title = title;
        this.department = department;
        this.officeLocation = officeLocation;
        this.jobDescription = jobDescription;
        this.symbol = symbol;
        this.phoneticName = phoneticName;
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

    private static final TextEnum SUBMAP = TextEnum.of(new Object[][] {
            {COL_FROM, SUB_FROM},
            {COL_TO, SUB_TO},
            {COL_COMPANY, SUB_COMPANY},
            {COL_TITLE, SUB_TITLE},
            {COL_DEPARTMENT, SUB_DEPARTMENT},
            {COL_OFFICE_LOCATION, SUB_OFFICE_LOCATION},
            {COL_JOB_DESCRIPTION, SUB_JOB_DESCRIPTION},
            {COL_SYMBOL, SUB_SYMBOL},
            {COL_PHONETIC_NAME, SUB_PHONETIC_NAME},
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
        return m;
    }

    @Override
    public void readProperties(Map<Integer, Object> props, boolean partial) {
        if (!partial || props.containsKey(SUB_FROM))
            setFrom(Values.toString(MapUtils.getObject(props, SUB_FROM, "")));
        if (!partial || props.containsKey(SUB_TO))
            setTo(Values.toString(MapUtils.getObject(props, SUB_TO, "")));
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
    }

    @Override
    public void deserialize(JsonNode jn) {
        setFrom(ObjectUtils.toString(jn.path(COL_FROM).getTextValue()));
        setTo(ObjectUtils.toString(jn.path(COL_TO).getTextValue()));
        setCompany(ObjectUtils.toString(jn.path(COL_COMPANY).getTextValue()));
        setTitle(ObjectUtils.toString(jn.path(COL_TITLE).getTextValue()));
        setDepartment(ObjectUtils.toString(jn.path(COL_DEPARTMENT).getTextValue()));
        setOfficeLocation(ObjectUtils.toString(jn.path(COL_OFFICE_LOCATION).getTextValue()));
        setJobDescription(ObjectUtils.toString(jn.path(COL_JOB_DESCRIPTION).getTextValue()));
        setSymbol(ObjectUtils.toString(jn.path(COL_SYMBOL).getTextValue()));
        setPhoneticName(ObjectUtils.toString(jn.path(COL_PHONETIC_NAME).getTextValue()));
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
        jg.writeStringField(COL_COMPANY, ObjectUtils.toString(getCompany()));
        jg.writeStringField(COL_TITLE, ObjectUtils.toString(getTitle()));
        jg.writeStringField(COL_DEPARTMENT, ObjectUtils.toString(getDepartment()));
        jg.writeStringField(COL_OFFICE_LOCATION, ObjectUtils.toString(getOfficeLocation()));
        jg.writeStringField(COL_JOB_DESCRIPTION, ObjectUtils.toString(getJobDescription()));
        jg.writeStringField(COL_SYMBOL, ObjectUtils.toString(getSymbol()));
        if (StringUtils.isNotEmpty(getPhoneticName()))
            jg.writeStringField(COL_PHONETIC_NAME, getPhoneticName());
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

        WorkHistory other = (WorkHistory) o;
        return StringUtils.equals(from, other.from)
                && StringUtils.equals(to, other.to)
                && StringUtils.equals(company, other.company)
                && StringUtils.equals(title, other.title)
                && StringUtils.equals(department, other.department)
                && StringUtils.equals(officeLocation, other.officeLocation)
                && StringUtils.equals(jobDescription, other.jobDescription)
                && StringUtils.equals(symbol, other.symbol)
                && StringUtils.equals(phoneticName, other.phoneticName);
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(from, to, company, title, department, officeLocation, jobDescription, symbol, phoneticName);
    }

    @Override
    public WorkHistory copy() {
        WorkHistory info = new WorkHistory();
        info.setFrom(from);
        info.setTo(to);
        info.setCompany(company);
        info.setTitle(title);
        info.setDepartment(department);
        info.setOfficeLocation(officeLocation);
        info.setJobDescription(jobDescription);
        info.setSymbol(symbol);
        info.setPhoneticName(phoneticName);
        return info;
    }
}
