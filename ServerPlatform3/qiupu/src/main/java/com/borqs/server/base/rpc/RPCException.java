package com.borqs.server.base.rpc;


import com.borqs.server.base.BaseException;
import com.borqs.server.base.ErrorCode;

public class RPCException extends BaseException {
    public RPCException() {
        super(ErrorCode.RPC_ERROR);
    }

    public RPCException(String format, Object... args) {
        super(ErrorCode.RPC_ERROR, format, args);
    }

    public RPCException(Throwable cause, String format, Object... args) {
        super(ErrorCode.RPC_ERROR, cause, format, args);
    }

    public RPCException(Throwable cause) {
        super(ErrorCode.RPC_ERROR, cause);
    }
}
