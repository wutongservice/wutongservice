package com.borqs.server.platform.feature.app;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.util.Encoders;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;

public class AppSign {


    public static final String PARAM_SIGN = "sign";
    public static final String PARAM_SIGN_METHOD = "sign_method";
    public static final String PARAM_APP_ID = "appid";
    public static final String PARAM_TICKET = "ticket";
    public static final String PARAM_JSONP_CALLBACK = "callback";
    public static final String PARAM_JSONP_PLACEHOLDER = "_";

    public static final String[] SPECIFIC_PARAMS = {
            PARAM_SIGN, PARAM_SIGN_METHOD, PARAM_APP_ID, PARAM_TICKET, PARAM_JSONP_CALLBACK, PARAM_JSONP_PLACEHOLDER,
    };

    public static String[] removeSpecialParams(String[] paramNames) {
        ArrayList<String> l = new ArrayList<String>();
        for (String pn : paramNames) {
            if (!ArrayUtils.contains(SPECIFIC_PARAMS, pn))
                l.add(pn);
        }
        return l.toArray(new String[l.size()]);
    }

    public static void removeSpecialParams(Collection<String> paramNames) {
        paramNames.remove(PARAM_SIGN);
        paramNames.remove(PARAM_SIGN_METHOD);
        paramNames.remove(PARAM_APP_ID);
        paramNames.remove(PARAM_TICKET);
        paramNames.remove(PARAM_JSONP_CALLBACK);
        paramNames.remove(PARAM_JSONP_PLACEHOLDER);
    }

    public static String md5(String appSecret, Collection<String> paramNames) {
        TreeSet<String> set = new TreeSet<String>(paramNames);
        removeSpecialParams(set);
        String joined = StringUtils.join(set, "");
        return Encoders.md5Base64(appSecret + joined + appSecret);
    }

    public static String sign(String signMethod, String appSecret, Collection<String> paramNames) {
        if ("md5".equalsIgnoreCase(signMethod))
            return md5(appSecret, paramNames);
        else
            throw new ServerException(E.INVALID_SIGN_METHOD);
    }

}
