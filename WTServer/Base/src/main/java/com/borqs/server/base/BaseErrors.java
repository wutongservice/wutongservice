package com.borqs.server.base;


import com.borqs.server.Errors;

public class BaseErrors extends Errors {

    public static final int PLATFORM_ERROR_CODE = 9000;

    public static final int PLATFORM_SFS_IO_ERROR = PLATFORM_ERROR_CODE + 101;
    public static final int PLATFORM_IO_ERROR = PLATFORM_ERROR_CODE + 102;
    public static final int PLATFORM_CLASS_NOT_FOUND_ERROR = PLATFORM_ERROR_CODE + 103;
    public static final int PLATFORM_NEW_INSTANCE_ERROR = PLATFORM_ERROR_CODE + 104;
    public static final int PLATFORM_NO_SUCH_METHOD_ERROR = PLATFORM_ERROR_CODE + 105;
    public static final int PLATFORM_INVOKE_METHOD_ERROR = PLATFORM_ERROR_CODE + 106;
    public static final int PLATFORM_FORMAT_TEXT_ERROR = PLATFORM_ERROR_CODE + 107;
    public static final int PLATFORM_EXECUTE_PROCESS_ERROR = PLATFORM_ERROR_CODE + 108;
    public static final int PLATFORM_ENCODE_OR_DECODE_ERROR = PLATFORM_ERROR_CODE + 109;
    public static final int PLATFORM_JSON_ERROR = PLATFORM_ERROR_CODE + 110;
    public static final int PLATFORM_IMAGE_PROCESS_ERROR = PLATFORM_ERROR_CODE + 111;


    public static final int PLATFORM_SQL_ERROR = PLATFORM_ERROR_CODE + 201;
    public static final int PLATFORM_SQL_TEMPLATE_ERROR = PLATFORM_ERROR_CODE + 202;
    public static final int PLATFORM_SQL_FOR_MIGRATE_ERROR = PLATFORM_ERROR_CODE + 203;

    public static final int PLATFORM_RUN_SERVER_ERROR = PLATFORM_ERROR_CODE + 301;
    public static final int PLATFORM_PARSE_QUERY_PARAMS_ERROR = PLATFORM_ERROR_CODE + 302;
    public static final int PLATFORM_LOAD_TEMPLATE_ERROR = PLATFORM_ERROR_CODE + 303;
    public static final int PLATFORM_MERGE_TEMPLATE_ERROR = PLATFORM_ERROR_CODE + 304;
    public static final int PLATFORM_INIT_TEMPLATES_ERROR = PLATFORM_ERROR_CODE + 305;
    public static final int PLATFORM_WEBMETHOD_REQUEST_ERROR = PLATFORM_ERROR_CODE + 306;
    public static final int PLATFORM_WEBMETHOD_GET_CONTENT_ERROR = PLATFORM_ERROR_CODE + 307;
    public static final int PLATFORM_WEBMETHOD_INIT_ERROR = PLATFORM_ERROR_CODE + 308;

    public static final int PLATFORM_MQ_INIT_ERROR = PLATFORM_ERROR_CODE + 401;
    public static final int PLATFORM_MQ_DESTROY_ERROR = PLATFORM_ERROR_CODE + 402;
    public static final int PLATFORM_MQ_SEND_ERROR = PLATFORM_ERROR_CODE + 403;
    public static final int PLATFORM_MQ_RECEIVE_ERROR = PLATFORM_ERROR_CODE + 404;

    public static final int PLATFORM_TRANSCEIVER_NOT_FOUND = PLATFORM_ERROR_CODE + 501;
    public static final int PLATFORM_INTERFACE_NOT_FOUND = PLATFORM_ERROR_CODE + 502;
    public static final int PLATFORM_TRANSCEIVER_CLOSE = PLATFORM_ERROR_CODE + 503;
    public static final int PLATFORM_GET_PROXY_ERROR = PLATFORM_ERROR_CODE + 504;

    public static final int PLATFORM_RECORD_ERROR = PLATFORM_ERROR_CODE + 601;
    public static final int PLATFORM_RECORD_SCHEMA_ERROR = PLATFORM_ERROR_CODE + 602;

    public static final int PLATFORM_PINYIN = PLATFORM_ERROR_CODE + 701;
    public static final int PLATFORM_CHSEG = PLATFORM_ERROR_CODE + 702;

    public static final int PLATFORM_ILLEGAL_PARAM = PLATFORM_ERROR_CODE + 777;
    public static final int PLATFORM_UNKNOWN_ERROR = PLATFORM_ERROR_CODE + 888;
    public static final int PLATFORM_GENERAL_ERROR = PLATFORM_ERROR_CODE + 999;


}
