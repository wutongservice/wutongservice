package com.borqs.server.base.io;


import com.borqs.server.ServerException;
import com.borqs.server.base.ErrorCode;

public class IOException2 extends ServerException {
    public IOException2() {
        super(ErrorCode.GENERAL_ERROR);
    }

    public IOException2(String format, Object... args) {
        super(ErrorCode.GENERAL_ERROR, format, args);
    }

    public IOException2(Throwable cause, String format, Object... args) {
        super(ErrorCode.GENERAL_ERROR, cause, format, args);
    }

    public IOException2(Throwable cause) {
        super(ErrorCode.GENERAL_ERROR, cause);
    }
}
