package com.borqs.server.wutong.notif;


import com.borqs.server.ServerException;
import com.borqs.server.wutong.WutongErrors;

public class NotificationException extends ServerException {
    public NotificationException() {
        super(WutongErrors.NOTIFICATION_SEND_ERROR);
    }

    public NotificationException(String format, Object... args) {
        super(WutongErrors.NOTIFICATION_SEND_ERROR, format, args);
    }

    public NotificationException(Throwable cause, String format, Object... args) {
        super(WutongErrors.NOTIFICATION_SEND_ERROR, cause, format, args);
    }

    public NotificationException(Throwable cause) {
        super(WutongErrors.NOTIFICATION_SEND_ERROR, cause);
    }
}
