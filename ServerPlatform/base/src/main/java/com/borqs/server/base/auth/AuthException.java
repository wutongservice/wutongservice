package com.borqs.server.base.auth;


import com.borqs.server.ServerException;
import com.borqs.server.base.ErrorCode;

public class AuthException extends ServerException {
    public AuthException() {
        super(ErrorCode.AUTH_ERROR);
    }

    public AuthException(String format, Object... args) {
        super(ErrorCode.AUTH_ERROR, format, args);
    }

    public AuthException(Throwable cause, String format, Object... args) {
        super(ErrorCode.AUTH_ERROR, cause, format, args);
    }

    public AuthException(Throwable cause) {
        super(ErrorCode.AUTH_ERROR, cause);
    }
}
