package com.borqs.server.market.utils.i18n;


import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;

public class CountryNames {

    public static String get(String code, String locale) {
        return get(code, LocaleUtils.toLocale(locale));
    }

    @SuppressWarnings("unchecked")
    public static String get(String code, Locale locale) {
        if (locale == null) {
            locale = Locale.US;
        }
        ResourceBundle bundle = ResourceBundle.getBundle("i18n.country", locale, new UTF8Control());
        try {
            return bundle.getString(code);
        } catch (Exception ignored) {
            return "";
        }
    }
}
