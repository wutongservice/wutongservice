package com.borqs.server.platform.util;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.data.Record;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

public class ParamChecker {
    public static <T> T notNull(String param, T v) {
        if (v == null)
            throw new ServerException(E.PARAM, "Param %s is null", param);
        return v;
    }

    public static String notEmpty(String param, String v) {
        if (StringUtils.isEmpty(v))
            throw new ServerException(E.PARAM, "Param %s is empty", param);
        return v;
    }

    public static String notBlank(String param, String v) {
        if (StringUtils.isBlank(v))
            throw new ServerException(E.PARAM, "Param %s is blank", param);
        return v;
    }

    public static void mustTrue(String param, boolean b, String err) {
        if (!b)
            throw new ServerException(E.PARAM, "Param %s: %s", param, err);
    }

    public static void mustFalse(String param, boolean b, String err) {
        if (b)
            throw new ServerException(E.PARAM, "Param %s: %s", param, err);
    }

    public static void mustHas(String param, Record rec, String col) {
        if (!rec.has(col))
            throw new ServerException(E.PARAM, "Param %s.%s is missing", param, col);
    }

    public static <T> void mustIn(String param, T v, T[] values) {
        if (!ArrayUtils.contains(values, v))
            throw new ServerException(E.PARAM, "Illegal param %s", v);
    }
}
