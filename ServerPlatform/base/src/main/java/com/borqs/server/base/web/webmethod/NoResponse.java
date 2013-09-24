package com.borqs.server.base.web.webmethod;


public final class NoResponse {
    private static final NoResponse INSTANCE = new NoResponse();

    public static NoResponse get() {
        return INSTANCE;
    }

    private NoResponse() {
    }
}
