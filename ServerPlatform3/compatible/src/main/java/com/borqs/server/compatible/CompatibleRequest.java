package com.borqs.server.compatible;


import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.request.Request;
import com.borqs.server.platform.feature.request.RequestTypes;
import com.borqs.server.platform.feature.request.Requests;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CompatibleRequest {

    public static final String V1TYPE_PROFILE_ACCESS = "1";
    //public static final String V1TYPE_FRIEND_FEEDBACK = "2";
    public static final String V1TYPE_ADD_FRIEND = "3";
    public static final String V1TYPE_CHANGE_MOBILE_TELEPHONE_NUMBER = "4";
    public static final String V1TYPE_CHANGE_MOBILE_2_TELEPHONE_NUMBER = "5";
    public static final String V1TYPE_CHANGE_MOBILE_3_TELEPHONE_NUMBER = "6";
    public static final String V1TYPE_CHANGE_EMAIL_ADDRESS = "7";
    public static final String V1TYPE_CHANGE_EMAIL_2_ADDRESS = "8";
    public static final String V1TYPE_CHANGE_EMAIL_3_ADDRESS = "9";

    public static final String CI_MOBILE_TELEPHONE_NUMBER = "mobile_telephone_number";
    public static final String CI_MOBILE_2_TELEPHONE_NUMBER = "mobile_2_telephone_number";
    public static final String CI_MOBILE_3_TELEPHONE_NUMBER = "mobile_3_telephone_number";
    public static final String CI_EMAIL_ADDRESS = "email_address";
    public static final String CI_EMAIL_2_ADDRESS = "email_2_address";
    public static final String CI_EMAIL_3_ADDRESS = "email_3_address";


    public static final String[] CI_TYPES = {
            CI_MOBILE_TELEPHONE_NUMBER,
            CI_MOBILE_2_TELEPHONE_NUMBER,
            CI_MOBILE_3_TELEPHONE_NUMBER,
            CI_EMAIL_ADDRESS,
            CI_EMAIL_2_ADDRESS,
            CI_EMAIL_3_ADDRESS,
    };

    public static final Map<String, String> CI_TYPE_MAP = new HashMap<String, String>();

    static {
        CI_TYPE_MAP.put(CI_MOBILE_TELEPHONE_NUMBER, V1TYPE_CHANGE_MOBILE_TELEPHONE_NUMBER);
        CI_TYPE_MAP.put(CI_MOBILE_2_TELEPHONE_NUMBER, V1TYPE_CHANGE_MOBILE_2_TELEPHONE_NUMBER);
        CI_TYPE_MAP.put(CI_MOBILE_3_TELEPHONE_NUMBER, V1TYPE_CHANGE_MOBILE_3_TELEPHONE_NUMBER);
        CI_TYPE_MAP.put(CI_EMAIL_ADDRESS, V1TYPE_CHANGE_EMAIL_ADDRESS);
        CI_TYPE_MAP.put(CI_EMAIL_2_ADDRESS, V1TYPE_CHANGE_EMAIL_2_ADDRESS);
        CI_TYPE_MAP.put(CI_EMAIL_3_ADDRESS, V1TYPE_CHANGE_EMAIL_3_ADDRESS);
    }

    public static String getChangeProfileV1Type(String ciKey) {
        return MapUtils.getString(CI_TYPE_MAP, ciKey, "");
    }

    public static final String V1COL_REQUEST_ID = "request_id";
    public static final String V1COL_SOURCE = "source";
    public static final String V1COL_APP = "app";
    public static final String V1COL_TYPE = "type";
    public static final String V1COL_CREATED_TIME = "created_time";
    public static final String V1COL_MESSAGE = "message";
    public static final String V1COL_DATA = "data";

    private static final String[] USER_V1_COLUMNS = {
            CompatibleUser.V1COL_USER_ID,
            CompatibleUser.V1COL_DISPLAY_NAME,
            CompatibleUser.V1COL_IMAGE_URL,
    };

    private static final CompatibleUser.UserJsonTransformer USER_TRANSFORMER = new CompatibleUser.UserJsonTransformer(USER_V1_COLUMNS);


    public static String requestsToJson(final Requests reqs, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializeRequests(jg, reqs);
            }
        }, human);
    }

    public static String requestToJson(final Request req, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializeRequest(jg, req);
            }
        }, human);
    }

    public static void serializeRequests(JsonGenerator jg, Requests reqs) throws IOException {
        jg.writeStartArray();
        for (Request req : reqs) {
            if (req != null)
                serializeRequest(jg, req);
        }
        jg.writeEndArray();
    }

    public static void serializeRequest(JsonGenerator jg, Request req) throws IOException {
        /*
        {
  "request_id" : 2812178982395200625,
  "source" : {
    "display_name" : "刘春荣",
    "small_image_url" : "http://storage.aliyun.com/wutong-photo/profile_10055_1338264877247_S.jpg",
    "image_url" : "http://storage.aliyun.com/wutong-photo/profile_10055_1338264877247_M.jpg",
    "large_image_url" : "http://storage.aliyun.com/wutong-photo/profile_10055_1338264877247_L.jpg",
    "contact_info" : {
      "email_address" : "chunrong.liu@borqs.com",
      "mobile_telephone_number" : "13811960980"
    },
    "remark" : "",
    "in_circles" : [ ],
    "profile_privacy" : false,
    "pedding_requests" : [ ],
    "uid" : 10055
  },
  "app" : 0,
  "type" : "1",
  "created_time" : 1340951434323,
  "message" : "",
  "data" : ""
}
         */
        String data = req.getData();
        jg.writeStartObject();
        jg.writeNumberField(V1COL_REQUEST_ID, req.getRequestId());
        req.writeAddonJsonAs(jg, Post.COL_SOURCE, V1COL_SOURCE, USER_TRANSFORMER);
        jg.writeNumberField(V1COL_APP, req.getApp());
        jg.writeStringField(V1COL_TYPE, v2ToV1Type(req.getType(), data));
        jg.writeNumberField(V1COL_CREATED_TIME, req.getCreatedTime());
        jg.writeStringField(V1COL_MESSAGE, req.getMessage());
        jg.writeFieldName(V1COL_DATA);
        String v1Data = v2ToV1Data(req.getType(), data);
        if (StringUtils.isEmpty(v1Data)) {
            jg.writeString("");
        } else {
            if (JsonHelper.isJson(v1Data))
                jg.writeRawValue(v1Data);
            else
                jg.writeString(v1Data);
        }
        jg.writeEndObject();
    }

    public static String v2ToV1Type(int v2Type, String data) {
        switch (v2Type) {
            case RequestTypes.REQ_CHANGE_PROFILE: {
                try {
                    JsonNode jn = JsonHelper.parse(data);
                    return jn.path("v1Type").getTextValue();
                } catch (Exception e) {
                    return "";
                }
            }

            case RequestTypes.REQ_EXCHANGE_VCARD:
                return V1TYPE_PROFILE_ACCESS;

            case RequestTypes.REQ_ADD_TO_FRIENDS:
                return V1TYPE_ADD_FRIEND;
        }
        return "";
    }

    public static String v2ToV1Data(int v2Type, String data) {
        if (v2Type == RequestTypes.REQ_CHANGE_PROFILE) {
            try {
                JsonNode jn = JsonHelper.parse(data);
                return jn.path("new").getTextValue();
            } catch (Exception e) {
                return "";
            }
        } else {
            return data;
        }
    }
}
