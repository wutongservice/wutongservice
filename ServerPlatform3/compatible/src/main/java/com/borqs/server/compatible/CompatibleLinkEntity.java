package com.borqs.server.compatible;


import com.borqs.server.platform.feature.link.LinkEntities;
import com.borqs.server.platform.feature.link.LinkEntity;
import com.borqs.server.platform.util.URLHelper;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.codehaus.jackson.JsonGenerator;

import java.io.IOException;

public class CompatibleLinkEntity {

    public static final String V1COL_URL = "url";
    public static final String V1COL_HOST = "host";
    public static final String V1COL_TITLE = "title";
    public static final String V1COL_DESCRIPTION = "description";
    public static final String V1COL_MANY_IMAGE_URL = "many_img_url";
    public static final String V1COL_IMAGE_URL = "img_url";

    public static String linksToJson(final LinkEntities les, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializeLinks(jg, les);
            }
        }, human);
    }

    public static String linkToJson(final LinkEntity le, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializeLink(jg, le);
            }
        }, human);
    }

    public static void serializeLinks(JsonGenerator jg, LinkEntities les) throws IOException {
        jg.writeStartArray();
        if (CollectionUtils.isNotEmpty(les)) {
            for (LinkEntity le : les) {
                if (le != null)
                    serializeLink(jg, le);
            }
        }
        jg.writeEndArray();
    }

    public static void serializeLink(JsonGenerator jg, LinkEntity le) throws IOException {
        // {"url":"http://news.sina.com.cn/c/p/2012-05-22/085324456521.shtml",
        // "host":"news.sina.com.cn","title":"Title",
        // "description":"Description",
        // "many_img_url":"[\"http://apitest.borqs.com/links/D:\\\\2workspace\\\\images2805295612429346864_small.jpg\",\"http://apitest.borqs.com/links/D:\\\\2workspace\\\\images2805295614227432625_small.jpg\"]",
        // "img_url":"http://apitest.borqs.com/links/D:\\2workspace\\images2805295612429346864_small.jpg"}
        jg.writeStartObject();
        jg.writeStringField(V1COL_URL, ObjectUtils.toString(le.getUrl()));
        jg.writeStringField(V1COL_HOST, URLHelper.getHost(le.getUrl()));
        jg.writeStringField(V1COL_TITLE, ObjectUtils.toString(le.getTitle()));
        jg.writeStringField(V1COL_DESCRIPTION, ObjectUtils.toString(le.getDescription()));
        jg.writeStringField(V1COL_IMAGE_URL, le.getImageUrl());
        jg.writeStringField(V1COL_MANY_IMAGE_URL, JsonHelper.toJson(le.getImageUrls() != null ? le.getImageUrls() : new String[0], false));
        jg.writeEndObject();
    }

}
