package com.borqs.server.compatible;


public class CompatibleTarget {

    public static final int V1_USER = 1;
    public static final int V1_POST = 2;
    public static final int V1_VIDEO = 3;
    public static final int V1_APK = 4;
    public static final int V1_MUSIC = 5;
    public static final int V1_BOOK = 6;
    public static final int V1_COMMENT = 7;
    public static final int V1_LIKE = 8;
    public static final int V1_LINK = 9;
    public static final int V1_PHOTO = 10;
    //public static final int V1_CONTACT = 11;

    public static int v1ToV2Type(int v1Type) {
        return v1Type;
    }

    public static int v2ToV1Type(int type) {
        return type;
    }

}
