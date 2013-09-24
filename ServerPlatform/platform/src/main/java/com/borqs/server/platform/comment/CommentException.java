package com.borqs.server.platform.comment;


import com.borqs.server.platform.ErrorCode;
import com.borqs.server.platform.PlatformException;

public class CommentException extends PlatformException {
    public CommentException() {
        super(ErrorCode.COMMENT_ERROR);
    }

    public CommentException(String format, Object... args) {
        super(ErrorCode.COMMENT_ERROR, format, args);
    }

    public CommentException(Throwable cause, String format, Object... args) {
        super(ErrorCode.COMMENT_ERROR, cause, format, args);
    }

    public CommentException(Throwable cause) {
        super(ErrorCode.COMMENT_ERROR, cause);
    }
}
