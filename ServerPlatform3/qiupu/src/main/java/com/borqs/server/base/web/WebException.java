package com.borqs.server.base.web;


import com.borqs.server.base.net.NetException;

public class WebException extends NetException {
    public WebException() {
    }

    public WebException(String format, Object... args) {
        super(format, args);
    }

    public WebException(Throwable cause, String format, Object... args) {
        super(cause, format, args);
    }

    public WebException(Throwable cause) {
        super(cause);
    }
}
