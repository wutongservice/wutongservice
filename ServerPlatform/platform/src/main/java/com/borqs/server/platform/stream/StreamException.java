package com.borqs.server.platform.stream;


import com.borqs.server.platform.ErrorCode;
import com.borqs.server.platform.PlatformException;

public class StreamException extends PlatformException {
    public StreamException() {
        super(ErrorCode.STREAM_ERROR);
    }

    public StreamException(String format, Object... args) {
        super(ErrorCode.STREAM_ERROR, format, args);
    }

    public StreamException(Throwable cause, String format, Object... args) {
        super(ErrorCode.STREAM_ERROR, cause, format, args);
    }

    public StreamException(Throwable cause) {
        super(ErrorCode.STREAM_ERROR, cause);
    }
}
