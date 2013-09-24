package com.borqs.server.base.sql;


import com.borqs.server.base.data.Null;
import com.borqs.server.base.data.Privacy;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import java.util.ArrayList;

public class SQLUtils {
    public static String toSql(Object o) {
        if (o instanceof CharSequence || o instanceof JsonNode) {
            return "'" + stringEscape(o.toString()) + "'";
        } else if (o instanceof Null || o instanceof Privacy) {
            return "null";
        } else {
            return ObjectUtils.toString(o, "null");
        }
    }

    public static String pageToLimit(int page, int count) {
        if (page <= 0) {
            return count >= 0 ? "LIMIT " + count : "";
        } else {
            return String.format("LIMIT %s,%s", pageToOffset(page, count), count >= 0 ? count : Integer.MAX_VALUE);
        }
    }

    public static int pageToOffset(int page, int count) {
        return page * count;
    }

    public static String stringEscape(String s) {
        if (s == null)
            return null;

        StringBuilder buff = new StringBuilder();
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            switch (c) {
                case 0:
                    buff.append("\\0");
                    break;
                case '\'':
                    buff.append("\\\'");
                    break;
                case '\"':
                    buff.append("\\\"");
                    break;
                case '\b':
                    buff.append("\\\b");
                    break;
                case '\n':
                    buff.append("\\\n");
                    break;
                case '\r':
                    buff.append("\\\r");
                    break;
                case '\t':
                    buff.append("\\\t");
                    break;
                case 26:
                    buff.append("\\Z");
                    break;
                case '\\':
                    buff.append("\\\\");
                    break;
                //case '%': buff.append("\\%"); break;
                //case '_': buff.append("\\_"); break;
                default:
                    buff.append(c);
            }
        }
        return buff.toString();
    }

    public static String valueJoin(String sep, Object... objs) {
        ArrayList<String> l = new ArrayList<String>();
        for (Object obj : objs) {
            l.add(toSql(obj));
        }
        return StringUtils.join(l, sep);
    }

}
