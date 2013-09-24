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

public class AddressInfo implements StringablePropertyBundle, JsonBean, Copyable<AddressInfo> {

    public static final String COL_TYPE = "type";
    public static final String COL_COUNTRY = "country";
    public static final String COL_PROVINCE = "province";
    public static final String COL_CITY = "city";
    public static final String COL_STREET = "street";
    public static final String COL_ZIP_CODE = "zip_code";
    public static final String COL_PO_BOX = "po_box";
    public static final String COL_NEIGHBORHOOD = "neighborhood";
    public static final String COL_LABEL = "label";

    public static final int SUB_TYPE = 1;
    public static final int SUB_COUNTRY = 2;
    public static final int SUB_PROVINCE = 3;
    public static final int SUB_CITY = 4;
    public static final int SUB_STREET = 5;
    public static final int SUB_ZIP_CODE = 6;
    public static final int SUB_PO_BOX = 7;
    public static final int SUB_NEIGHBORHOOD = 8;
    public static final int SUB_LABEL = 9;

    public static final String TYPE_HOME = "home";
    public static final String TYPE_WORK = "work";
    public static final String TYPE_OTHER = "other";
    public static final String[] TYPES = {TYPE_HOME, TYPE_WORK, TYPE_OTHER};

    private String type = "";
    private String country = "";
    private String province = "";
    private String city = "";
    private String street = "";
    private String zipCode = "";
    private String poBox = "";
    private String neighborhood = "";
    private String label = "";

    public AddressInfo() {
    }

    public AddressInfo(String type, String country, String province, String city, String street, String zipCode, String poBox, String neighborhood, String label) {
        this.type = type;
        this.country = country;
        this.province = province;
        this.city = city;
        this.street = street;
        this.zipCode = zipCode;
        this.poBox = poBox;
        this.neighborhood = neighborhood;
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getPoBox() {
        return poBox;
    }

    public void setPoBox(String poBox) {
        this.poBox = poBox;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    private static final TextEnum SUBMAP = TextEnum.of(new Object[][]{
            {COL_TYPE, SUB_TYPE},
            {COL_COUNTRY, SUB_COUNTRY},
            {COL_PROVINCE, SUB_PROVINCE},
            {COL_CITY, SUB_CITY},
            {COL_STREET, SUB_STREET},
            {COL_ZIP_CODE, SUB_ZIP_CODE},
            {COL_PO_BOX, SUB_PO_BOX},
            {COL_NEIGHBORHOOD, SUB_NEIGHBORHOOD},
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
        if (StringUtils.isNotEmpty(getCountry()))
            m.put(SUB_COUNTRY, getCountry());
        if (StringUtils.isNotEmpty(getProvince()))
            m.put(SUB_PROVINCE, getProvince());
        if (StringUtils.isNotEmpty(getCity()))
            m.put(SUB_CITY, getCity());
        if (StringUtils.isNotEmpty(getStreet()))
            m.put(SUB_STREET, getStreet());
        if (StringUtils.isNotEmpty(getZipCode()))
            m.put(SUB_ZIP_CODE, getZipCode());
        if (StringUtils.isNotEmpty(getPoBox()))
            m.put(SUB_PO_BOX, getPoBox());
        if (StringUtils.isNotEmpty(getNeighborhood()))
            m.put(SUB_NEIGHBORHOOD, getNeighborhood());
        if (StringUtils.isNotEmpty(getLabel()))
            m.put(SUB_LABEL, getLabel());
        return m;
    }

    @Override
    public void readProperties(Map<Integer, Object> props, boolean partial) {
        if (!partial || props.containsKey(SUB_TYPE))
            setType(Values.toString(MapUtils.getObject(props, SUB_TYPE, "")));
        if (!partial || props.containsKey(SUB_COUNTRY))
            setCountry(Values.toString(MapUtils.getObject(props, SUB_COUNTRY, "")));
        if (!partial || props.containsKey(SUB_PROVINCE))
            setProvince(Values.toString(MapUtils.getObject(props, SUB_PROVINCE, "")));
        if (!partial || props.containsKey(SUB_CITY))
            setCity(Values.toString(MapUtils.getObject(props, SUB_CITY, "")));
        if (!partial || props.containsKey(SUB_STREET))
            setStreet(Values.toString(MapUtils.getObject(props, SUB_STREET, "")));
        if (!partial || props.containsKey(SUB_ZIP_CODE))
            setZipCode(Values.toString(MapUtils.getObject(props, SUB_ZIP_CODE, "")));
        if (!partial || props.containsKey(SUB_PO_BOX))
            setPoBox(Values.toString(MapUtils.getObject(props, SUB_PO_BOX, "")));
        if (!partial || props.containsKey(SUB_NEIGHBORHOOD))
            setNeighborhood(Values.toString(MapUtils.getObject(props, SUB_NEIGHBORHOOD, "")));
        if (!partial || props.containsKey(SUB_LABEL))
            setLabel(Values.toString(MapUtils.getObject(props, SUB_LABEL, "")));
    }

    @Override
    public void deserialize(JsonNode jn) {
        setType(ObjectUtils.toString(jn.path(COL_TYPE).getTextValue()));
        setCountry(ObjectUtils.toString(jn.path(COL_COUNTRY).getTextValue()));
        setProvince(ObjectUtils.toString(jn.path(COL_PROVINCE).getTextValue()));
        setCity(ObjectUtils.toString(jn.path(COL_CITY).getTextValue()));
        setStreet(ObjectUtils.toString(jn.path(COL_STREET).getTextValue()));
        setZipCode(ObjectUtils.toString(jn.path(COL_ZIP_CODE).getTextValue()));
        setPoBox(ObjectUtils.toString(jn.path(COL_PO_BOX).getTextValue()));
        setNeighborhood(ObjectUtils.toString(jn.path(COL_NEIGHBORHOOD).getTextValue()));
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
        jg.writeStringField(COL_COUNTRY, ObjectUtils.toString(getCountry()));
        jg.writeStringField(COL_PROVINCE, ObjectUtils.toString(getProvince()));
        jg.writeStringField(COL_CITY, ObjectUtils.toString(getCity()));
        jg.writeStringField(COL_STREET, ObjectUtils.toString(getStreet()));
        jg.writeStringField(COL_ZIP_CODE, ObjectUtils.toString(getZipCode()));
        jg.writeStringField(COL_PO_BOX, ObjectUtils.toString(getPoBox()));
        if (StringUtils.isNotEmpty(getNeighborhood()))
            jg.writeStringField(COL_NEIGHBORHOOD, ObjectUtils.toString(getNeighborhood()));
        if (StringUtils.isNotEmpty(getLabel()))
            jg.writeStringField(COL_LABEL, ObjectUtils.toString(getLabel()));
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

        AddressInfo other = (AddressInfo) o;
        return StringUtils.equals(type, other.type)
                && StringUtils.equals(country, other.country)
                && StringUtils.equals(province, other.province)
                && StringUtils.equals(city, other.city)
                && StringUtils.equals(street, other.street)
                && StringUtils.equals(zipCode, other.zipCode)
                && StringUtils.equals(poBox, other.poBox)
                && StringUtils.equals(neighborhood, other.neighborhood)
                && StringUtils.equals(label, other.label);
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(type, country, province, city, street, zipCode, poBox, neighborhood, label);
    }

    @Override
    public AddressInfo copy() {
        AddressInfo info = new AddressInfo();
        info.setType(type);
        info.setCountry(country);
        info.setProvince(province);
        info.setCity(city);
        info.setStreet(street);
        info.setZipCode(zipCode);
        info.setPoBox(poBox);
        info.setNeighborhood(neighborhood);
        info.setLabel(label);
        return info;
    }
}
