package com.borqs.server.platform.suggesteduser;

import com.borqs.server.platform.ErrorCode;
import com.borqs.server.platform.PlatformException;

public class SuggestedUserException extends PlatformException {
    public SuggestedUserException() {
        super(ErrorCode.SUGGESTEDUSER_ERROR);
    }

    public SuggestedUserException(String format, Object... args) {
        super(ErrorCode.SUGGESTEDUSER_ERROR, format, args);
    }

    public SuggestedUserException(Throwable cause, String format, Object... args) {
        super(ErrorCode.SUGGESTEDUSER_ERROR, cause, format, args);
    }

    public SuggestedUserException(Throwable cause) {
        super(ErrorCode.SUGGESTEDUSER_ERROR, cause);
    }
}
