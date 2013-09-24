package com.borqs.server.platform.web.topaz;


public class HaltException extends RuntimeException {
    public final int status;
    public final String body;

    public HaltException(int status, String body) {
        this.status = status;
        this.body = body;
    }
}
