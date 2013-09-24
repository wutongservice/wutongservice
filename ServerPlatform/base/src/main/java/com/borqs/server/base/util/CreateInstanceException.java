package com.borqs.server.base.util;


import com.borqs.server.base.BaseException;
import com.borqs.server.base.ErrorCode;

public class CreateInstanceException extends BaseException {
    public CreateInstanceException() {
        super(ErrorCode.GENERAL_ERROR);
    }

    public CreateInstanceException(String format, Object... args) {
        super(ErrorCode.GENERAL_ERROR, format, args);
    }

    public CreateInstanceException(Throwable cause, String format, Object... args) {
        super(ErrorCode.GENERAL_ERROR, cause, format, args);
    }

    public CreateInstanceException(Throwable cause) {
        super(ErrorCode.GENERAL_ERROR, cause);
    }
}
