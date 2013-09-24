package com.borqs.server.platform.app;


import com.borqs.server.platform.ErrorCode;
import com.borqs.server.platform.PlatformException;

public class AppException extends PlatformException {
    public AppException() {
        super(ErrorCode.APP_ERROR);
    }

    public AppException(String format, Object... args) {
        super(ErrorCode.APP_ERROR, format, args);
    }

    public AppException(Throwable cause, String format, Object... args) {
        super(ErrorCode.APP_ERROR, cause, format, args);
    }

    public AppException(Throwable cause) {
        super(ErrorCode.APP_ERROR, cause);
    }
}
