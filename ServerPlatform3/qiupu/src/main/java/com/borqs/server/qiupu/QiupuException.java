package com.borqs.server.qiupu;


import com.borqs.server.ServerException;

public class QiupuException extends ServerException {
    public QiupuException(int code) {
        super(code);
    }

    public QiupuException(int code, String format, Object... args) {
        super(code, format, args);
    }

    public QiupuException(int code, Throwable cause, String format, Object... args) {
        super(code, cause, format, args);
    }

    public QiupuException(int code, Throwable cause) {
        super(code, cause);
    }
}
