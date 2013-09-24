package com.borqs.server.market.models;


import com.borqs.server.market.utils.StringUtils2;
import org.apache.commons.lang.StringUtils;

public class Tags {
    public static String trimTags(String tags) {
        if (StringUtils.isBlank(tags))
            return "";

        String[] ss = StringUtils2.splitArray(tags, ',', true);
        return StringUtils.join(ss, ',');
    }
}
