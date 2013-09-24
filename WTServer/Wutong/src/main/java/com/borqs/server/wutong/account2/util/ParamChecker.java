package com.borqs.server.wutong.account2.util;


import com.borqs.server.ServerException;
import com.borqs.server.base.data.Record;
import com.borqs.server.wutong.WutongErrors;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

public class ParamChecker {
    public static <T> T notNull(String param, T v) {
        if (v == null)
            throw new ServerException(WutongErrors.SYSTEM_MISS_REQUIRED_PARAMETER, "Param %s is null", param);
        return v;
    }

    public static String notEmpty(String param, String v) {
        if (StringUtils.isEmpty(v))
            throw new ServerException(WutongErrors.SYSTEM_MISS_REQUIRED_PARAMETER, "Param %s is empty", param);
        return v;
    }

    public static String notBlank(String param, String v) {
        if (StringUtils.isBlank(v))
            throw new ServerException(WutongErrors.SYSTEM_MISS_REQUIRED_PARAMETER, "Param %s is blank", param);
        return v;
    }

    public static void shouldTrue(String param, boolean b, String err) {
        if (!b)
            throw new ServerException(WutongErrors.SYSTEM_MISS_REQUIRED_PARAMETER, "Param %s: %s", param, err);
    }

    public static void shouldFalse(String param, boolean b, String err) {
        if (b)
            throw new ServerException(WutongErrors.SYSTEM_MISS_REQUIRED_PARAMETER, "Param %s: %s", param, err);
    }

    public static void shouldHas(String param, Record rec, String col) {
        if (!rec.has(col))
            throw new ServerException(WutongErrors.SYSTEM_MISS_REQUIRED_PARAMETER, "Param %s.%s is missing", param, col);
    }

    public static <T> void shouldIn(String param, T v, T[] values) {
        if (!ArrayUtils.contains(values, v))
            throw new ServerException(WutongErrors.SYSTEM_MISS_REQUIRED_PARAMETER, "Illegal param %s", v);
    }
}
