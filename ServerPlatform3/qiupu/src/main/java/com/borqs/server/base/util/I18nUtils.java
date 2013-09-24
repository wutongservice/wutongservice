package com.borqs.server.base.util;

import java.util.Locale;
import java.util.ResourceBundle;

public class I18nUtils {
    public static String getBundleStringByLang(String lang, String key)
    {
        ResourceBundle bundle = I18nHelper.getBundle("com.borqs.server.platform.i18n.platform", new Locale(lang));
        return bundle.getString(key);
    }
}