package com.borqs.server.service.notification;


import com.borqs.server.base.BaseException;
import com.borqs.server.base.ErrorCode;

public class NotificationException extends BaseException {
    public NotificationException() {
        super(ErrorCode.NOTIFICATION_ERROR);
    }

    public NotificationException(String format, Object... args) {
        super(ErrorCode.NOTIFICATION_ERROR, format, args);
    }

    public NotificationException(Throwable cause, String format, Object... args) {
        super(ErrorCode.NOTIFICATION_ERROR, cause, format, args);
    }

    public NotificationException(Throwable cause) {
        super(ErrorCode.NOTIFICATION_ERROR, cause);
    }
}
