package com.borqs.server.base.util;


import com.borqs.server.base.BaseException;
import com.borqs.server.base.ErrorCode;

public class EncoderException extends BaseException {
    public EncoderException() {
        super(ErrorCode.ENCODER_ERROR);
    }

    public EncoderException(String format, Object... args) {
        super(ErrorCode.ENCODER_ERROR, format, args);
    }

    public EncoderException(Throwable cause, String format, Object... args) {
        super(ErrorCode.ENCODER_ERROR, cause, format, args);
    }

    public EncoderException(Throwable cause) {
        super(ErrorCode.ENCODER_ERROR, cause);
    }
}
