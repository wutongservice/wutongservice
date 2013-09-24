package com.borqs.server.base.util;


import com.borqs.server.base.BaseException;
import com.borqs.server.base.ErrorCode;

public class ProcessException extends BaseException {
    public ProcessException() {
        super(ErrorCode.PROCESS_ERROR);
    }

    public ProcessException(String format, Object... args) {
        super(ErrorCode.PROCESS_ERROR, format, args);
    }

    public ProcessException(Throwable cause, String format, Object... args) {
        super(ErrorCode.PROCESS_ERROR, cause, format, args);
    }

    public ProcessException(Throwable cause) {
        super(ErrorCode.PROCESS_ERROR, cause);
    }
}
