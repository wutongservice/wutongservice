package com.borqs.server.platform.photo;


import com.borqs.server.platform.ErrorCode;
import com.borqs.server.platform.PlatformException;

public class PhotoException extends PlatformException {
    public PhotoException() {
        super(ErrorCode.ACCOUNT_ERROR);
    }

    public PhotoException(String format, Object... args) {
        super(ErrorCode.ACCOUNT_ERROR, format, args);
    }

    public PhotoException(Throwable cause, String format, Object... args) {
        super(ErrorCode.ACCOUNT_ERROR, cause, format, args);
    }

    public PhotoException(Throwable cause) {
        super(ErrorCode.ACCOUNT_ERROR, cause);
    }
}
