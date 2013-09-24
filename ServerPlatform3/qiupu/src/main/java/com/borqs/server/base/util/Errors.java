package com.borqs.server.base.util;


import com.borqs.server.ServerException;
import com.borqs.server.base.ErrorCode;
import com.borqs.server.base.ResponseError;

public class Errors {
    public static ResponseError createResponseError(int code, String msg, Object... args) {
        ResponseError err = new ResponseError();
        err.code = code;
        err.message = String.format(msg, args);
        return err;
    }

    public static ResponseError wrapResponseError(Throwable t) {
        if (t instanceof IllegalArgumentException) {
            IllegalArgumentException e = (IllegalArgumentException)t;
            return createResponseError(ErrorCode.PARAM_ERROR, e.getMessage());
        } if (t instanceof ResponseError) {
            return (ResponseError)t;
        } else if (t instanceof ServerException)  {
            ServerException e = (ServerException)t;
            return createResponseError(e.code, e.getMessage());
        } else {
            return createResponseError(ErrorCode.UNKNOWN_ERROR, t.getMessage());
        }
    }
}
