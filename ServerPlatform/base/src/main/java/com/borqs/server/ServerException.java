package com.borqs.server;


public class ServerException extends RuntimeException {
    public final int code;

    public ServerException(int code) {
        super("");
        this.code = code;
    }

    public ServerException(int code, String format, Object... args) {
        super(String.format(format, args));
        this.code = code;
    }

    public ServerException(int code, Throwable cause, String format, Object... args) {
        super(String.format(format, args), cause);
        this.code = code;
    }

    public ServerException(int code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public static ServerException wrap(int code, Throwable cause) {
        return wrap(code, cause, "");
    }
    public static ServerException wrap(int code, Throwable cause, String message, Object... args) {
        return cause instanceof ServerException ? (ServerException) cause : new ServerException(code, cause, message, args);
    }
}
