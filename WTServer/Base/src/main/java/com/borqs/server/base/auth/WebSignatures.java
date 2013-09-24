package com.borqs.server.base.auth;


import com.borqs.server.base.util.Encoders;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.util.Collection;
import java.util.TreeSet;

public class WebSignatures {
    public static String md5Sign(String appSecret, Collection<String> paramNames) {
        TreeSet<String> set = new TreeSet<String>(paramNames);
        set.remove("$ua");
        set.remove("$loc");
        set.remove("$lang");
        set.remove("$ec");
        set.remove("ticket");
        set.remove("appid");
        set.remove("sign");
        set.remove("sign_method");
        set.remove("callback");
        set.remove("generate_time");
        set.remove("_");
        return md5Sign(appSecret, StringUtils.join(set, ""));
    }

    public static String md5Sign(String appSecret, String s) {
        Validate.notNull(appSecret);
        Validate.notNull(s);
        return Encoders.md5Base64(appSecret + s + appSecret);
    }
}
