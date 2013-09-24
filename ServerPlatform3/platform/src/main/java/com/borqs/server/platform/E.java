package com.borqs.server.platform;


public interface E {
    int OK = 0;

    int PARAM = 1;
    int UNKNOWN = 2;
    int UNSUPPORTED = 3;
    int MISSING_HTTP_HEADER = 10;
    int DECODE_HTTP_HEADER = 11;

    int DATA = 900;
    int JSON = 902;
    int SQL = 904;
    int CLASS = 905;
    int TEMPLATE = 906;
    int CHARSET = 907;
    int ENCODE = 908;
    int VFS = 909;
    int WEB_SERVER = 910;
    int WEB_CLIENT = 911;
    int SERVICE = 912;
    int TOPAZ = 913;
    int IMAGE = 914;
    int EMAIL = 915;
    int SFS = 916;
    int IO = 917;
    int CACHE = 918;
    int JMX = 919;
    int SLEEP = 920;
    int RPC = 921;
    int URL_PATTERN = 922;
    int PROCESS = 800;
    int TEST = 801;
    int MACRO_EXPAND = 803;
    int IMAGE_MAGICK = 806;
    int FTS = 807;
    int CHSEG = 808;
    int PINYIN = 809;

    // login/cibind/account
    int INVALID_USER = 104;
    int NOT_LOGIN = 105;
    int INVALID_TICKET = 106;
    int FORMAT_USER = 107;
    int WRITE_USER = 108;
    int INVALID_USER_OR_PASSWORD = 109;
    int INVALID_BINDING_TYPE = 110;
    int TOO_MANY_BINDING = 111;
    int BINDING_EXISTS = 112;
    int INVALID_SIGN_METHOD = 113;
    int INVALID_SIGN = 114;

    // verify
    int TOO_MANY_VERIFICATION_REQUEST = 115;
    int INVALID_VERIFICATION_CODE = 116;
    int TOO_MANY_VERIFICATION = 117;

    // friend/circle
    int TOO_MANY_CIRCLES = 120;
    int INVALID_CIRCLE = 121;



    // stream
    int INVALID_POST = 130;
    int REPETITIVE_REPOST = 131;
    int TO_MANY_PEOPLEID = 132;

    // privacy
    int INVALID_PRIVACY_ALLOWED_MODE = 140;

    // comment
    int CANNOT_COMMENT = 150;

    // comment
    int CANNOT_QUOTE = 160;

    // like
    int CANNOT_LIKE = 170;

    // opline
    int VARIOUS_OPERATOR = 180;

    // photo
    int INVALID_ALBUM = 190;
    int SAVE_PHOTO = 191;

    // link
    int FETCH_LINK = 200;
    int RESIZE_LINK_IMAGE = 201;

    // qiupu
    int INVALID_APK = 210;

    // notif
    int SEND_NOTIF_ERROR = 220;

    // group
    int CREATE_GROUP_ID = 230;
    int MEMBER_LIMIT_ERROR = 231;
    int CREATE_GROUP = 232;

//    int STREAM = 201;
//    int COMMENT = 202;
//    int LIKE = 203;
//    int PRIVACY = 204;
//    int FRIEND = 205;
//    int SIGN = 198;
//    int APP = 199;
}
