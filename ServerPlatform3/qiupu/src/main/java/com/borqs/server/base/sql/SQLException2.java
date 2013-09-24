package com.borqs.server.base.sql;


import com.borqs.server.base.BaseException;
import com.borqs.server.base.ErrorCode;

public class SQLException2 extends BaseException {
    public SQLException2() {
        super(ErrorCode.DATA_ERROR);
    }

    public SQLException2(String format, Object... args) {
        super(ErrorCode.DATA_ERROR, format, args);
    }

    public SQLException2(Throwable cause, String format, Object... args) {
        super(ErrorCode.DATA_ERROR, cause, format, args);
    }

    public SQLException2(Throwable cause) {
        super(ErrorCode.DATA_ERROR, cause);
    }
}
