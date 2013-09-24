package com.borqs.server.platform.like;


import com.borqs.server.platform.ErrorCode;
import com.borqs.server.platform.PlatformException;

public class LikeException extends PlatformException {
    public LikeException() {
        super(ErrorCode.LIKE_ERROR);
    }

    public LikeException(String format, Object... args) {
        super(ErrorCode.LIKE_ERROR, format, args);
    }

    public LikeException(Throwable cause, String format, Object... args) {
        super(ErrorCode.LIKE_ERROR, cause, format, args);
    }

    public LikeException(Throwable cause) {
        super(ErrorCode.LIKE_ERROR, cause);
    }
}
