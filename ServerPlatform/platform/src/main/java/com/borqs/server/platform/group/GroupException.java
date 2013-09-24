package com.borqs.server.platform.group;

import com.borqs.server.platform.ErrorCode;
import com.borqs.server.platform.PlatformException;

public class GroupException extends PlatformException {
    public GroupException() {
        super(ErrorCode.GROUP_ERROR);
    }

    public GroupException(String format, Object... args) {
        super(ErrorCode.GROUP_ERROR, format, args);
    }

    public GroupException(Throwable cause, String format, Object... args) {
        super(ErrorCode.GROUP_ERROR, cause, format, args);
    }

    public GroupException(Throwable cause) {
        super(ErrorCode.GROUP_ERROR, cause);
    }
}
