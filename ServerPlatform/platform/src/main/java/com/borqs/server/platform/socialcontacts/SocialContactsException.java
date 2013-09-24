package com.borqs.server.platform.socialcontacts;

import com.borqs.server.platform.ErrorCode;
import com.borqs.server.platform.PlatformException;

public class SocialContactsException extends PlatformException {
    public SocialContactsException() {
        super(ErrorCode.SOCIALCONTACTS_ERROR);
    }

    public SocialContactsException(String format, Object... args) {
        super(ErrorCode.SOCIALCONTACTS_ERROR, format, args);
    }

    public SocialContactsException(Throwable cause, String format, Object... args) {
        super(ErrorCode.SOCIALCONTACTS_ERROR, cause, format, args);
    }

    public SocialContactsException(Throwable cause) {
        super(ErrorCode.SOCIALCONTACTS_ERROR, cause);
    }
}
