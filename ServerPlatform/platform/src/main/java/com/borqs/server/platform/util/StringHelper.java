package com.borqs.server.platform.util;


import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.util.*;

public class StringHelper {

    public static String toCamelNome(String s) {
        if (StringUtils.isBlank(s))
            return StringUtils.trimToEmpty(s);

        String[] segs = StringUtils.split(s, "_");
        StringBuilder buff = new StringBuilder();
        for (String seg : segs) {
            if (StringUtils.isNotBlank(seg)) {
                if (buff.length() == 0)
                    buff.append(StringUtils.uncapitalize(sameCase(seg) ? seg.toLowerCase() : seg));
                else
                    buff.append(StringUtils.capitalize(sameCase(seg) ? seg.toLowerCase() : seg));
            }
        }
        return buff.toString();
    }

    public static boolean sameCase(String s) {
        Validate.notNull(s);
        if (StringUtils.isBlank(s))
            return true;

        int len = s.length();
        int firstAlphaCase = -1; // 0 -> Upper, 1 -> Lower
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (CharUtils.isAsciiAlpha(c)) {
                if (firstAlphaCase == -1) {
                    firstAlphaCase = CharUtils.isAsciiAlphaUpper(c) ? 0 : 1;
                } else {
                    int theCase = CharUtils.isAsciiAlphaUpper(c) ? 0 : 1;
                    if (theCase != firstAlphaCase)
                        return false;
                }

            }
        }
        return true;
    }


    public static String[] splitArray(String s, String sep, boolean strip) {
        List<String> l = splitList(s, sep, strip);
        return l.toArray(new String[l.size()]);
    }

    public static List<String> splitList(String s, String sep, boolean strip) {
        ArrayList<String> l = new ArrayList<String>();
        for (String e : StringUtils.split(s, sep)) {
            String e0 = strip ? StringUtils.strip(e) : e;
            if (!StringUtils.isEmpty(e0))
                l.add(e0);
        }
        return l;
    }

    public static Set<String> splitSet(String s, String sep, boolean strip) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        for (String e : StringUtils.split(s, sep)) {
            String e0 = strip ? StringUtils.strip(e) : e;
            if (!StringUtils.isEmpty(e0))
                set.add(e0);
        }
        return set;
    }

    public static List<Long> splitLongList(String s, String sep) {
        ArrayList<Long> l = new ArrayList<Long>();
        for (String e : StringUtils.split(s, sep)) {
            String e0 = StringUtils.strip(e);
            if (!StringUtils.isEmpty(e0))
                l.add(Long.parseLong(e0));
        }
        return l;
    }

    public static int[] splitIntArray(String s, String sep) {
        ArrayList<Integer> l = new ArrayList<Integer>();
        for (String e : StringUtils.split(s, sep)) {
            String e0 = StringUtils.strip(e);
            if (!StringUtils.isEmpty(e0))
                l.add(Integer.parseInt(e0));
        }
        return ArrayUtils.toPrimitive(l.toArray(new Integer[l.size()]));
    }

    public static long[] splitLongArray(String s, String sep) {
        ArrayList<Long> l = new ArrayList<Long>();
        for (String e : StringUtils.split(s, sep)) {
            String e0 = StringUtils.strip(e);
            if (!StringUtils.isEmpty(e0))
                l.add(Long.parseLong(e0));
        }
        return ArrayUtils.toPrimitive(l.toArray(new Long[l.size()]));
    }

    public static Set<Long> splitLongSet(String s, String sep) {
        HashSet<Long> l = new HashSet<Long>();
        for (String e : StringUtils.split(s, sep)) {
            String e0 = StringUtils.strip(e);
            if (!StringUtils.isEmpty(e0))
                l.add(Long.parseLong(e0));
        }
        return l;
    }

//
//    public static long[] splitIntArray(String s, String sep) {
//        List<Long> l = splitIntList(s, sep);
//        Long[] a = l.toArray(new Long[l.size()]);
//        return ArrayUtils.toPrimitive(a);
//    }

    public static String joinIgnoreNull(Object... objs) {
        StringBuilder buff = new StringBuilder();
        for (Object obj : objs) {
            if (obj != null)
                buff.append(obj.toString());
        }
        return buff.toString();
    }

    public static String indent(int indent) {
        return indent > 0 ? StringUtils.repeat("  ", indent) : "";
    }

    public static String doIndent(String s, int indent) {
        String indentStr = indent(indent);
        StringBuilder buff = new StringBuilder();
        String[] lines = StringUtils.split(StringUtils.trimToEmpty(s), '\n');
        for (String line : lines)
            buff.append(indentStr).append(line).append('\n');

        return buff.toString();
    }

    public static String join(int[] a, String sep) {
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0)
                buff.append(sep);
            buff.append(a[i]);
        }
        return buff.toString();
    }

    public static String join(long[] a, String sep) {
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0)
                buff.append(sep);
            buff.append(a[i]);
        }
        return buff.toString();
    }

    public static String join(Object[] a, String sep) {
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0)
                buff.append(sep);
            buff.append(a[i]);
        }
        return buff.toString();
    }

    public static String join(Object[] a) {
        return join(a, "");
    }

    public static String removeStartAndEnd(String s, String start, String end) {
        s = StringUtils.removeStart(s, start);
        return StringUtils.removeEnd(s, end);
    }

    public static String addPrefix(String s, String prefix,String replace,String defaultIcon, boolean checkReduplicated) {
        if(StringUtils.isNotEmpty(replace)){
            if(defaultIcon == null){
                defaultIcon = "";
            }
            prefix = prefix.replace(replace,defaultIcon);
        }
        s = StringUtils.trimToEmpty(s);
        if (checkReduplicated) {
            return !s.startsWith(prefix) ? prefix + s : s;
        } else {
            return prefix + s;
        }
    }
}
