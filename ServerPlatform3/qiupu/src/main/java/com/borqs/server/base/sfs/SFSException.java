package com.borqs.server.base.sfs;


import com.borqs.server.base.BaseException;
import com.borqs.server.base.ErrorCode;

public class SFSException extends BaseException {
    public SFSException() {
        super(ErrorCode.SFS_ERROR);
    }

    public SFSException(String format, Object... args) {
        super(ErrorCode.SFS_ERROR, format, args);
    }

    public SFSException(Throwable cause, String format, Object... args) {
        super(ErrorCode.SFS_ERROR, cause, format, args);
    }

    public SFSException(Throwable cause) {
        super(ErrorCode.SFS_ERROR, cause);
    }
}
