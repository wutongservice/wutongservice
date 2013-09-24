package com.borqs.server.base.util;

import org.apache.commons.lang.StringUtils;

public class StringValidator {
    public static boolean validatePhone(String phone) {
        if (StringUtils.isNotBlank(phone)
                && phone.matches("(1[3,5,8][\\d]{9})"))
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
