package com.borqs.server.base.util;

import org.apache.commons.lang.StringUtils;

public class PhoneNumberHelper {

    public static final int MOBILE_PHONE_NUMBER_LENGTH = "13812345678".length();

    public static String stripMobilePhoneNumber(String phone) {
        phone = StringUtils.trimToEmpty(phone);
        phone = StringUtils.remove(phone, " ");
        return phone.length() > MOBILE_PHONE_NUMBER_LENGTH
                ? StringUtils.right(phone, MOBILE_PHONE_NUMBER_LENGTH)
                : phone;
    }

    public static String makeMobilePhoneNumber(String phone, String countryCode) {
        countryCode = StringUtils.trimToEmpty(countryCode);
        if (!countryCode.startsWith("+"))
            countryCode = "+" + countryCode;

        return countryCode + stripMobilePhoneNumber(phone);
    }
}
