package com.borqs.server.base.mq;


import com.borqs.server.base.BaseException;
import com.borqs.server.base.ErrorCode;

public class MQException extends BaseException {
    public MQException() {
        super(ErrorCode.MQ_ERROR);
    }

    public MQException(String format, Object... args) {
        super(ErrorCode.MQ_ERROR, format, args);
    }

    public MQException(Throwable cause, String format, Object... args) {
        super(ErrorCode.MQ_ERROR, cause, format, args);
    }

    public MQException(Throwable cause) {
        super(ErrorCode.MQ_ERROR, cause);
    }
}
