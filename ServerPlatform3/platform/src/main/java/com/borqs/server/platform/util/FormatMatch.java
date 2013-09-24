package com.borqs.server.platform.util;


public class FormatMatch {

    public static boolean isEmail(String s) {
        return s != null && s.matches("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
    }

    public static boolean isPhone(String s) {
        s = PhoneNumberHelper.stripMobilePhoneNumber(s);
        return s.matches("(13[\\d]{9})");
    }
}
