package com.borqs.server;

public interface E {
    int OK = 0;

    int PARAM = 1;
    int UNKNOWN = 2;

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
    int IMAGE_MAGICK = 806;

    // login/cibind/account
    int INVALID_USER = 104;
    int NOT_LOGIN = 105;
    int INVALID_TICKET = 106;
    int FORMAT_USER = 107;
    int WRITE_USER = 108;
    int INVALID_USER_OR_PASSWORD = 109;
    int INVALID_BINDING_TYPE = 110;
    int TOO_MANY_BINDING = 111;
    int WRITE_BINDING = 112;
    int INVALID_SIGN_METHOD = 113;
    int INVALID_SIGN = 114;

    // verify
    int TOO_MANY_VERIFICATION_REQUEST = 115;
    int INVALID_VERIFICATION_CODE = 116;
    int TOO_MANY_VERIFICATION = 117;


//    int STREAM = 201;
//    int COMMENT = 202;
//    int LIKE = 203;
//    int PRIVACY = 204;
//    int FRIEND = 205;
//    int SIGN = 198;
//    int APP = 199;
}