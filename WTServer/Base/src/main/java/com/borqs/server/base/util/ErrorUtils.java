package com.borqs.server.base.util;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorUtils {
    public static ServerException wrapResponseError(Throwable t) {
        if (t instanceof IllegalArgumentException) {
            IllegalArgumentException e = (IllegalArgumentException)t;
            return new ServerException(BaseErrors.PLATFORM_ILLEGAL_PARAM, e.getMessage());
        } else if (t instanceof ServerException)  {
            return (ServerException) t;
        } else {
            return new ServerException(BaseErrors.PLATFORM_UNKNOWN_ERROR, t.getMessage());
        }
    }

    public static String getStackTrace(Throwable t) {
        if (t == null)
            return "";

        StringWriter w = new StringWriter();
        PrintWriter out = new PrintWriter(w);
        t.printStackTrace(out);
        out.flush();
        return w.toString();
    }
}
