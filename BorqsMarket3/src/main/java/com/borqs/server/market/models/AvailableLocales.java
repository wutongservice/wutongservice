package com.borqs.server.market.models;


import com.borqs.server.market.utils.JsonUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class AvailableLocales {
    public static final String[] LOCALES = {
            "en_US",
            "zh_CN",
    };

    public static String joinedLocales() {
        return StringUtils.join(LOCALES, ",");
    }

    public static Map<String, Object> multipleLocales(Object val) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
        for (String locale : LOCALES) {
            m.put(locale, val);
        }
        return m;
    }

    public static JsonNode multipleLocalesAsJsonNode(Object val) throws IOException {
        return JsonUtils.toJsonNode(multipleLocales(val));
    }

    public static String multipleLocalesAsJson(Object val, boolean pretty) throws IOException {
        return JsonUtils.toJson(multipleLocales(val), pretty);
    }
}
