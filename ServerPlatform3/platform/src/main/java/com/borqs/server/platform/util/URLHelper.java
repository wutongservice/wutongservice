package com.borqs.server.platform.util;


import org.apache.commons.lang.ObjectUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLHelper {
    public static String getHost(String url) {
        try {
            return new URL(url).getHost();
        } catch (MalformedURLException e) {
            return "";
        }
    }

    private static final Pattern HTTP_URL_PATT = Pattern.compile("(http|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\!\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\!\\@?^=%&amp;/~\\+#])?", Pattern.CASE_INSENSITIVE);
    public static String catchHttpUrl(String text) {
        Matcher m = HTTP_URL_PATT.matcher(ObjectUtils.toString(text));
        if (m.find())
            return m.group();
        else
            return "";
    }
}
