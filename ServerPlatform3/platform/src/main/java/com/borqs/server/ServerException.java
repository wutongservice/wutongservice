package com.borqs.server;


public class ServerException extends RuntimeException {
    public final int code;

    public ServerException(int code) {
        this.code = code;
    }

    public ServerException(int code, String message, Object... args) {
        super(String.format(message, args));
        this.code = code;
    }

    public ServerException(int code, Throwable cause, String message, Object... args) {
        super(String.format(message, args), cause);
        this.code = code;
    }

    public ServerException(int code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ServerException wrap(int code, Throwable cause) {
        return wrap(code, cause, "");
    }

    public static ServerException wrap(int code, Throwable cause, String message, Object... args) {
        return cause instanceof ServerException ? (ServerException) cause : new ServerException(code, cause, message, args);
    }
}
