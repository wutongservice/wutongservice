package com.borqs.server.qiupu.util.apkinfo;

import com.borqs.server.qiupu.ErrorCode;
import com.borqs.server.qiupu.QiupuException;


public class ApkInfoException extends QiupuException {
    public ApkInfoException() {
        super(ErrorCode.APK_INFO_ERROR);
    }

    public ApkInfoException(String format, Object... args) {
        super(ErrorCode.APK_INFO_ERROR, format, args);
    }

    public ApkInfoException(Throwable cause, String format, Object... args) {
        super(ErrorCode.APK_INFO_ERROR, cause, format, args);
    }

    public ApkInfoException(Throwable cause) {
        super(ErrorCode.APK_INFO_ERROR, cause);
    }
}
