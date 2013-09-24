package com.borqs.server.platform.poll;

import com.borqs.server.platform.ErrorCode;
import com.borqs.server.platform.PlatformException;

public class PollException extends PlatformException {
    public PollException() {
        super(ErrorCode.POLL_ERROR);
    }

    public PollException(String format, Object... args) {
        super(ErrorCode.POLL_ERROR, format, args);
    }

    public PollException(Throwable cause, String format, Object... args) {
        super(ErrorCode.POLL_ERROR, cause, format, args);
    }

    public PollException(Throwable cause) {
        super(ErrorCode.POLL_ERROR, cause);
    }
}
