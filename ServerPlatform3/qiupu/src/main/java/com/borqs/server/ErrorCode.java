package com.borqs.server;


public class ErrorCode {
    // Basic errors
    public static final int PARAM_ERROR = 1;
    public static final int GENERAL_ERROR = 998;
    public static final int UNKNOWN_ERROR = 999;

    public static final int BIND_ERROR = 400;

    protected static final int BASE_ERROR = 100;
    protected static final int PLATFORM_ERROR = 200;
    protected static final int QIUPU_ERROR = 300;

    protected static final int FILE_ERROR = 600;

    public static final int JSON_ERROR = BASE_ERROR + 1;
    public static final int DATA_ERROR = BASE_ERROR + 2;
    public static final int ENCODER_ERROR = BASE_ERROR + 3;
    public static final int RPC_ERROR = BASE_ERROR + 4;
    public static final int NET_ERROR = BASE_ERROR + 5;
    public static final int AUTH_ERROR = BASE_ERROR + 6;
    public static final int SFS_ERROR = BASE_ERROR + 7;
    public static final int IMAGE_ERROR = BASE_ERROR + 8;
    public static final int PROCESS_ERROR = BASE_ERROR + 9;
    public static final int NOTIFICATION_ERROR = BASE_ERROR + 10;
    public static final int MQ_ERROR = BASE_ERROR + 11;

    public static final int ACCOUNT_ERROR = PLATFORM_ERROR + 1;
    public static final int FRIENDSHIP_ERROR = PLATFORM_ERROR + 2;
    public static final int STREAM_ERROR = PLATFORM_ERROR + 3;
    public static final int COMMENT_ERROR = PLATFORM_ERROR + 4;
    public static final int LIKE_ERROR = PLATFORM_ERROR + 5;
    public static final int APP_ERROR = PLATFORM_ERROR + 6;
    public static final int SUGGESTEDUSER_ERROR = PLATFORM_ERROR + 7;
    public static final int SOCIALCONTACTS_ERROR = PLATFORM_ERROR + 8;
    public static final int LOGIN_NAME_OR_PASSWORD_ERROR = PLATFORM_ERROR + 9;
    public static final int LOGIN_NAME_EXISTS = PLATFORM_ERROR + 10;
    public static final int USER_NOT_EXISTS = PLATFORM_ERROR + 11;
    public static final int GENERATE_USER_ID_ERROR = PLATFORM_ERROR + 12;
    public static final int CREATE_SESSION_ERROR = PLATFORM_ERROR + 13;
    public static final int CONVERSATION_ERROR = PLATFORM_ERROR + 13;
    public static final int FORMAT_USER_ERROR = PLATFORM_ERROR + 14;
    public static final int GROUP_ERROR = PLATFORM_ERROR + 15;

    public static final int VERIFICATION_ERROR = PLATFORM_ERROR + 20;
    public static final int REQUEST_TOO_FREQUENT = PLATFORM_ERROR + 21;
    public static final int SEND_SMS_ERROR = PLATFORM_ERROR + 22;
    public static final int VERIFY_TOO_FREQUENT = PLATFORM_ERROR + 23;

    public static final int BIND_HASBIND_ERROR = BIND_ERROR+1;
    public static final int BIND_BINDMANY_ERROR = BIND_ERROR+2;
    public static final int BIND_KEY_ERROR = BIND_ERROR+3;
    public static final int BIND_PHONE_ERROR = BIND_ERROR+4;
    public static final int  SQL = 904;
    public static final int  INVALID_USER_OR_PASSWORD = 109;
    public static final int  WRITE_USER = 108;
    public static final int  PARAM = 1;

}
