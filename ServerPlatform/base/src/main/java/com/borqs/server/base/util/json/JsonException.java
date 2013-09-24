package com.borqs.server.base.util.json;


import com.borqs.server.base.BaseException;
import com.borqs.server.base.ErrorCode;

public class JsonException extends BaseException {
    public JsonException() {
        super(ErrorCode.JSON_ERROR);
    }

    public JsonException(String format, Object... args) {
        super(ErrorCode.JSON_ERROR, format, args);
    }

    public JsonException(Throwable cause, String format, Object... args) {
        super(ErrorCode.JSON_ERROR, cause, format, args);
    }

    public JsonException(Throwable cause) {
        super(ErrorCode.JSON_ERROR, cause);
    }
}
