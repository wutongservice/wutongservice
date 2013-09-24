package com.borqs.server.qiupu.util.apkinfo;

import com.borqs.server.ServerException;
import com.borqs.server.qiupu.QiupuErrors;


public class ApkInfoException extends ServerException {
    public ApkInfoException() {
        super(QiupuErrors.APK_INFO_ERROR);
    }

    public ApkInfoException(String format, Object... args) {
        super(QiupuErrors.APK_INFO_ERROR, format, args);
    }

    public ApkInfoException(Throwable cause, String format, Object... args) {
        super(QiupuErrors.APK_INFO_ERROR, cause, format, args);
    }

    public ApkInfoException(Throwable cause) {
        super(QiupuErrors.APK_INFO_ERROR, cause);
    }
}
