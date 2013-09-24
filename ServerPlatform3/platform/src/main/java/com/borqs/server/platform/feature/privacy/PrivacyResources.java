package com.borqs.server.platform.feature.privacy;


import org.apache.commons.lang.StringUtils;

public class PrivacyResources {
    public static final String RES_VCARD = "vcard";

    public static final String[] RESOURCES = {
        RES_VCARD,
    };

    public static final String JOINED_RESOURCES = StringUtils.join(RESOURCES, ",");
}
