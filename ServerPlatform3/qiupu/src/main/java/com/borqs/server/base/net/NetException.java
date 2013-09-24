package com.borqs.server.base.net;


import com.borqs.server.base.BaseException;
import com.borqs.server.base.ErrorCode;

public class NetException extends BaseException {
    public NetException() {
        super(ErrorCode.NET_ERROR);
    }

    public NetException(String format, Object... args) {
        super(ErrorCode.NET_ERROR, format, args);
    }

    public NetException(Throwable cause, String format, Object... args) {
        super(ErrorCode.NET_ERROR, cause, format, args);
    }

    public NetException(Throwable cause) {
        super(ErrorCode.NET_ERROR, cause);
    }
}
