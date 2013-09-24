package com.borqs.server.platform.feature.psuggest;

public class SuggestionReasons {
    public static final int REASON_NONE = 0;
    public static final int RECOMMENDER_USER = 10;
    public static final int REQUEST_ATTENTION = 12;
    public static final int FROM_ADDRESS = 20;
    public static final int FROM_ADDRESS_HAVE_BORQSID = 21;
    public static final int FROM_ADDRESS_HAVE_COMMON_CONTACT = 22;
    public static final int FROM_ADDRESS_HAVE_MY_CONTACT = 23;
    public static final int FROM_USERPROFILE = 30;
    public static final int FROM_USERPROFILE_WORKINFO = 31;
    public static final int FROM_USERPROFILE_EDUINFO = 32;
    public static final int IN_COMMON_FRIENDS = 40;
    public static final int FROM_SYSTEM = 50;

    public static final int[] REASONS = {
            RECOMMENDER_USER,
            REQUEST_ATTENTION,
            FROM_ADDRESS,
            FROM_ADDRESS_HAVE_BORQSID,
            FROM_ADDRESS_HAVE_COMMON_CONTACT,
            FROM_ADDRESS_HAVE_MY_CONTACT,
            FROM_USERPROFILE,
            FROM_USERPROFILE_WORKINFO,
            FROM_USERPROFILE_EDUINFO,
            IN_COMMON_FRIENDS,
            FROM_SYSTEM
    };
}
