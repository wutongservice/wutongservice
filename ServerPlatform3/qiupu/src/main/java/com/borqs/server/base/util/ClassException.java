package com.borqs.server.base.util;


import com.borqs.server.base.BaseException;
import com.borqs.server.base.ErrorCode;

public class ClassException extends BaseException {
    public ClassException() {
        super(ErrorCode.GENERAL_ERROR);
    }

    public ClassException(String format, Object... args) {
        super(ErrorCode.GENERAL_ERROR, format, args);
    }

    public ClassException(Throwable cause, String format, Object... args) {
        super(ErrorCode.GENERAL_ERROR, cause, format, args);
    }

    public ClassException(Throwable cause) {
        super(ErrorCode.GENERAL_ERROR, cause);
    }
}
