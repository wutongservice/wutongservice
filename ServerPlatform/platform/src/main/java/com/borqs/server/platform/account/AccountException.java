package com.borqs.server.platform.account;


import com.borqs.server.platform.ErrorCode;
import com.borqs.server.platform.PlatformException;

public class AccountException extends PlatformException {
    public AccountException(int code) {
        super(code);
    }

    public AccountException(int code, String format, Object... args) {
        super(code, format, args);
    }

    public AccountException(int code, Throwable cause, String format, Object... args) {
        super(code, cause, format, args);
    }

    public AccountException(int code, Throwable cause) {
        super(code, cause);
    }
}
