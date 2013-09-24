package com.borqs.server.base.util;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import com.borqs.server.base.util.json.JsonUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

public class FeedbackParams extends LinkedHashMap<String, String> {

    public FeedbackParams() {
    }

    public boolean has(String key) {
        return containsKey(key);
    }

    @Override
    public String get(Object key) {
        return StringUtils.trimToEmpty(super.get(key));
    }

    @Override
    public String put(String key, String value) {
        return super.put(key, StringUtils.trimToEmpty(value));
    }

    public FeedbackParams set(String key, String value) {
        put(key, value);
        return this;
    }

    @Override
    public String toString() {
        return JsonUtils.toJson(this, true);
    }

    public String toBase64(boolean encrypt) {
        String s = JsonUtils.toJson(this, false);
        return encrypt ? Encoders.desEncryptBase64(s) : Encoders.toBase64(s);
    }

    public static FeedbackParams fromBase64(String base64) {
        String s = debase64(base64);
        return JsonUtils.fromJson(s, FeedbackParams.class);
    }

    protected static String debase64(String base64) {
        String s;
        try {
            s = Encoders.desDecryptFromBase64(base64);
        } catch (Throwable t) {
            try {
                s = Encoders.stringFromBase64(base64);
            } catch (Throwable t2) {
                throw new ServerException(BaseErrors.PLATFORM_ENCODE_OR_DECODE_ERROR, "Debase64 error");
            }
        }
        return s;
    }


    public static String toSegmentedBase64(boolean encrypt, String sep, String... segs) {
        String s = StringUtils.join(segs, sep);
        return encrypt ? Encoders.desEncryptBase64(s) : Encoders.toBase64(s);
    }

    public static String[] fromSegmentedBase64(String base64, String sep) {
        String s = debase64(base64);
        return s.split(sep);
    }
    
    public static String[] fromSegmentedBase64(String base64, String sep, int len) {
        String s = debase64(base64);
        return StringUtils2.splitArray(s, sep, len , true);
    }

    public static String[] fromRegexCapturedBase64(String base64, String regex) {
        String s = debase64(base64);
        Pattern patt = Pattern.compile(regex);
        java.util.regex.Matcher m = patt.matcher(s);
        ArrayList<String> l = new ArrayList<String>();
        if (m.find()) {
            int groupCount = m.groupCount();
            for (int i = 1; i <= groupCount; i++)
                l.add(m.group(i));
        }
        return l.toArray(new String[l.size()]);
    }
}
