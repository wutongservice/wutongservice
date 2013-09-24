package com.borqs.server.platform;


import com.borqs.server.base.BaseException;

public class PlatformException extends BaseException {
    public PlatformException(int code) {
        super(code);
    }

    public PlatformException(int code, String format, Object... args) {
        super(code, format, args);
    }

    public PlatformException(int code, Throwable cause, String format, Object... args) {
        super(code, cause, format, args);
    }

    public PlatformException(int code, Throwable cause) {
        super(code, cause);
    }
}
