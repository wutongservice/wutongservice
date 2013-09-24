package com.borqs.server.base;


import com.borqs.server.ServerException;

public class BaseException extends ServerException {
    public BaseException(int code) {
        super(code);
    }

    public BaseException(int code, String format, Object... args) {
        super(code, format, args);
    }

    public BaseException(int code, Throwable cause, String format, Object... args) {
        super(code, cause, format, args);
    }

    public BaseException(int code, Throwable cause) {
        super(code, cause);
    }
}
