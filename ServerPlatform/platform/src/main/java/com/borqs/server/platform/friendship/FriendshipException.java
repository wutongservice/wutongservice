package com.borqs.server.platform.friendship;


import com.borqs.server.platform.ErrorCode;
import com.borqs.server.platform.PlatformException;

public class FriendshipException extends PlatformException {
    public FriendshipException() {
        super(ErrorCode.FRIENDSHIP_ERROR);
    }

    public FriendshipException(String format, Object... args) {
        super(ErrorCode.FRIENDSHIP_ERROR, format, args);
    }

    public FriendshipException(Throwable cause, String format, Object... args) {
        super(ErrorCode.FRIENDSHIP_ERROR, cause, format, args);
    }

    public FriendshipException(Throwable cause) {
        super(ErrorCode.FRIENDSHIP_ERROR, cause);
    }
}
