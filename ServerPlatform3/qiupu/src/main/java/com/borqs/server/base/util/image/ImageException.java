package com.borqs.server.base.util.image;


import com.borqs.server.base.BaseException;
import com.borqs.server.base.ErrorCode;

public class ImageException extends BaseException {
    public ImageException() {
        super(ErrorCode.IMAGE_ERROR);
    }

    public ImageException(String format, Object... args) {
        super(ErrorCode.IMAGE_ERROR, format, args);
    }

    public ImageException(Throwable cause, String format, Object... args) {
        super(ErrorCode.IMAGE_ERROR, cause, format, args);
    }

    public ImageException(Throwable cause) {
        super(ErrorCode.IMAGE_ERROR, cause);
    }
}
