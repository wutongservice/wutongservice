package com.borqs.server.base.io;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class Charsets {
    public static final String DEFAULT = "UTF-8";
    public static final Charset DEFAULT_CHARSET = Charset.forName(DEFAULT);

    public static byte[] toBytes(String s) {
        try {
            return s.getBytes(DEFAULT);
        } catch (UnsupportedEncodingException e) {
            throw new ServerException(BaseErrors.PLATFORM_IO_ERROR, e);
        }
    }

    public static String fromBytes(byte[] bytes) {
        return fromBytes(bytes, 0, bytes.length);
    }

    public static String fromBytes(byte[] bytes, int off, int len) {
        try {
            return new String(bytes, off, len, DEFAULT);
        } catch (UnsupportedEncodingException e) {
            throw new ServerException(BaseErrors.PLATFORM_IO_ERROR, e);
        }
    }
}
