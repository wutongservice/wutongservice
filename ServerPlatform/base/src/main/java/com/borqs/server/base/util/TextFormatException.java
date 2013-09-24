package com.borqs.server.base.util;


import com.borqs.server.base.BaseException;
import com.borqs.server.base.ErrorCode;

public class TextFormatException extends BaseException {
    public TextFormatException() {
        super(ErrorCode.GENERAL_ERROR);
    }

    public TextFormatException(String format, Object... args) {
        super(ErrorCode.GENERAL_ERROR, format, args);
    }

    public TextFormatException(Throwable cause, String format, Object... args) {
        super(ErrorCode.GENERAL_ERROR, cause, format, args);
    }

    public TextFormatException(Throwable cause) {
        super(ErrorCode.GENERAL_ERROR, cause);
    }
}
