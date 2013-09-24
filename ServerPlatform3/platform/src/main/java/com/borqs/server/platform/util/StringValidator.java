package com.borqs.server.platform.util;

import org.apache.commons.lang.StringUtils;

public class StringValidator {
    public static boolean validatePhone(String phone) {
        if (StringUtils.isNotBlank(phone)
                && phone.matches("(1[\\d]{10})"))
            return true;
        else
            return false;
    }

    public static boolean validateEmail(String email) {
        if (StringUtils.isNotBlank(email)
                && email.matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$"))
            return true;
        else
            return false;
    }
}
