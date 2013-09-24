package com.borqs.server.market;


import org.apache.commons.lang.ObjectUtils;

public class ServiceException extends Exception {
    private final int code;
    private String[] details;

    public ServiceException(int code) {
        this.code = code;
    }

    public ServiceException(int code, String message) {
        super(ObjectUtils.toString(message));
        this.code = code;
    }

    public ServiceException(int code, String message, Throwable cause) {
        super(ObjectUtils.toString(message), cause);
        this.code = code;
    }

    public ServiceException(int code, Throwable cause) {
        super("", cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public String[] getDetails() {
        return details;
    }

    public void setDetails(String[] details) {
        this.details = details;
    }

    public ServiceException withDetails(String... details) {
        setDetails(details);
        return this;
    }
}
