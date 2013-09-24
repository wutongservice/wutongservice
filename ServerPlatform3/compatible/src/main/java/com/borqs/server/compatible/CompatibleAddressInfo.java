package com.borqs.server.compatible;


import com.borqs.server.platform.feature.account.AddressInfo;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CompatibleAddressInfo {

    public static final String V1COL_POSTAL_CODE = "postal_code";
    public static final String V1COL_STREET = "street";
    public static final String V1COL_STATE = "state";
    public static final String V1COL_TYPE = "type";
    public static final String V1COL_PO_BOX = "po_box";
    public static final String V1COL_EXTENDED_ADDRESS = "extended_address";
    public static final String V1COL_CITY = "city";
    public static final String V1COL_COUNTRY = "country";

    public static List<AddressInfo> jsonToAddressInfos(List<AddressInfo> reuse, String json) {
        return jsonNodeToAddressInfos(reuse, JsonHelper.parse(json));
    }

    public static AddressInfo jsonToAddressInfo(String json) {
        return jsonNodeToAddressInfo(JsonHelper.parse(json));
    }

    public static List<AddressInfo> jsonNodeToAddressInfos(List<AddressInfo> reuse, JsonNode jn) {
        if (reuse == null)
            reuse = new ArrayList<AddressInfo>();
        for (int i = 0; i < jn.size(); i++)
            reuse.add(jsonNodeToAddressInfo(jn.get(i)));
        return reuse;
    }

    public static AddressInfo jsonNodeToAddressInfo(JsonNode jn) {
        AddressInfo addr = new AddressInfo();
        deserializeAddressInfo(jn, addr);
        return addr;
    }

    public static void deserializeAddressInfo(JsonNode jn, AddressInfo addr) {
        if (jn.has(V1COL_TYPE))
            addr.setType(v1ToV2Type(jn.path(V1COL_TYPE).getTextValue()));
        if (jn.has(V1COL_POSTAL_CODE))
            addr.setZipCode(jn.path(V1COL_POSTAL_CODE).getTextValue());
        if (jn.has(V1COL_STREET))
            addr.setStreet(jn.path(V1COL_STREET).getTextValue());
        if (jn.has(V1COL_STATE))
            addr.setProvince(jn.path(V1COL_STATE).getTextValue());
        if (jn.has(V1COL_PO_BOX))
            addr.setPoBox(jn.path(V1COL_PO_BOX).getTextValue());
        if (jn.has(V1COL_COUNTRY))
            addr.setCountry(jn.path(V1COL_COUNTRY).getTextValue());
    }

    public static void serializeAddressInfo(JsonGenerator jg, List<AddressInfo> addrs) throws IOException {
        jg.writeStartArray();
        if (CollectionUtils.isNotEmpty(addrs)) {
            for (AddressInfo addr : addrs) {
                if (addr != null)
                    serializeAddonsInfo(jg, addr);
            }
        }
        jg.writeEndArray();
    }

    public static void serializeAddonsInfo(JsonGenerator jg, AddressInfo addr) throws IOException {
        // {"postal_code":"","street":"","state":"","type":"","po_box":"","extended_address":"","city":"","country":""}
        jg.writeStartObject();
        jg.writeStringField(V1COL_TYPE, v2ToV1Type(ObjectUtils.toString(addr.getType())));
        jg.writeStringField(V1COL_POSTAL_CODE, ObjectUtils.toString(addr.getZipCode()));
        jg.writeStringField(V1COL_STREET, ObjectUtils.toString(addr.getStreet()));
        jg.writeStringField(V1COL_STATE, ObjectUtils.toString(addr.getProvince()));
        jg.writeStringField(V1COL_PO_BOX, ObjectUtils.toString(addr.getPoBox()));
        jg.writeStringField(V1COL_EXTENDED_ADDRESS, "");
        jg.writeStringField(V1COL_CITY, ObjectUtils.toString(addr.getCity()));
        jg.writeStringField(V1COL_COUNTRY, ObjectUtils.toString(addr.getCountry()));
        jg.writeEndObject();
    }

    public static String addressToJson(final AddressInfo addr, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializeAddonsInfo(jg, addr);
            }
        }, human);
    }

    public static String addressesToJson(final List<AddressInfo> addrs, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializeAddressInfo(jg, addrs);
            }
        }, human);
    }

    public static String v1ToV2Type(String v1Type) {
        return v1Type;
    }

    public static String v2ToV1Type(String type) {
        return type;
    }
}
