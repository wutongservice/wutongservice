package com.borqs.server.platform.util;


import org.apache.commons.lang.ArrayUtils;

public class EntityFormat {

    public static String joinFormat(Format format, String sep, Object[] ids) {
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < ids.length; i++) {
            if (i > 0)
                buff.append(sep);

            buff.append(format.format(ids[i]));
        }
        return buff.toString();
    }

    public static String joinFormat(Format format, String sep, long... ids) {
        return joinFormat(format, sep, ArrayUtils.toObject(ids));
    }

    public static String joinFormat(Format format, String sep, String... ids) {
        return joinFormat(format, sep, (Object[]) ids);
    }


    public static String joinFormat(Format format, String sep, String omit, int max, Object[] ids) {
        if (max <= 0) {
            return joinFormat(format, sep, ids);
        } else {
            StringBuilder buff = new StringBuilder();
            for (int i = 0; i < ids.length; i++) {
                if (i < max) {
                    if (i > 0)
                        buff.append(sep);
                    buff.append(format.format(ids[i]));
                } else {
                    buff.append(omit);
                    break;
                }
            }
            return buff.toString();
        }
    }

    public static String joinFormat(Format format, String sep, String omit, int max, long... ids) {
        return joinFormat(format, sep, omit, max, ArrayUtils.toObject(ids));
    }

    public static String joinFormat(Format format, String sep, String omit, int max, String... ids) {
        return joinFormat(format, sep, omit, max, (Object[])ids);
    }

    public static interface Format {
        String format(Object id);
    }
}
