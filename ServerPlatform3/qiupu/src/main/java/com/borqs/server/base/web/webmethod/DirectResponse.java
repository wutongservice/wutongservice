package com.borqs.server.base.web.webmethod;


import org.apache.commons.lang.ObjectUtils;

public final class DirectResponse {
    public final String contentType;
    public final String content;


    private DirectResponse(String contentType, String content) {
        this.contentType = contentType;
        this.content = content;
    }

    public static DirectResponse of(String contentType, Object o) {
        return new DirectResponse(ObjectUtils.toString(contentType, "text/html"), ObjectUtils.toString(o, ""));
    }

    public static DirectResponse of(Object o) {
        return of("text/html", o);
    }
}
