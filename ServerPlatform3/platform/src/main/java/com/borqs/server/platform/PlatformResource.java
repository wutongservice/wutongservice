package com.borqs.server.platform;

import com.borqs.server.platform.util.I18nHelper;

import java.util.Locale;
import java.util.ResourceBundle;


public class PlatformResource {

    public static final String CIRCLE_NAME_BLOCKED = "circle.name.blocked";
    public static final String CIRCLE_NAME_ADDRESS_BOOK = "circle.name.address_book";
    public static final String CIRCLE_NAME_DEFAULT = "circle.name.default";
    public static final String CIRCLE_NAME_FAMILY = "circle.name.family";
    public static final String CIRCLE_NAME_ACQUAINTANCE = "circle.name.acquaintance";
    public static final String CIRCLE_NAME_CLOSED_FRIENDS = "circle.name.closed_friends";


    public static ResourceBundle getResource(Locale loc) {
        return I18nHelper.getBundle("com.borqs.server.platform.i18n.Platform", loc);
    }

    public static ResourceBundle getResource(String loc) {
        return getResource(I18nHelper.parseLocale(loc));
    }
}
