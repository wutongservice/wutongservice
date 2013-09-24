package com.borqs.server.base.data;


import com.borqs.server.base.BaseException;
import com.borqs.server.base.ErrorCode;

public class DataException extends BaseException {
    public DataException() {
        super(ErrorCode.DATA_ERROR);
    }

    public DataException(String format, Object... args) {
        super(ErrorCode.DATA_ERROR, format, args);
    }

    public DataException(Throwable cause, String format, Object... args) {
        super(ErrorCode.DATA_ERROR, cause, format, args);
    }

    public DataException(Throwable cause) {
        super(ErrorCode.DATA_ERROR, cause);
    }
}
