package com.borqs.server.platform.util;


import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static String[] splitArray(String s, String sep, int len, boolean strip) {
        String[] arr0 = new String[len];
        for(int i = 0; i < len; i++)
        {
            arr0[i] = "";
        }

        String[] arr1 = s.split(sep);

        for(int i = 0; i < arr1.length; i++)
        {
            if(strip)
                arr1[i] = StringUtils.strip(arr1[i]);

            arr0[i] = arr1[i];
        }

        return arr0;
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

    public static long[] splitLongArrayAsSet(String s, String sep) {
        LinkedHashSet<Long> l = new LinkedHashSet<Long>();
        for (String e : StringUtils.split(s, sep)) {
            String e0 = StringUtils.strip(e);
            if (!StringUtils.isEmpty(e0))
                l.add(Long.parseLong(e0));
        }
        return ArrayUtils.toPrimitive(l.toArray(new Long[l.size()]));
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

    public static String addPrefix(String s, String prefix, boolean checkReduplicated) {
        s = StringUtils.trimToEmpty(s);
        if (checkReduplicated) {
            return !s.startsWith(prefix) ? prefix + s : s;
        } else {
            return prefix + s;
        }
    }

    public static String replaceRegex(String s, Pattern patt, ReplaceHandler handler) {
        if (s == null)
            return null;

        Matcher m = patt.matcher(s);
        if (m.find()) {
            m.reset();
            StringBuilder buff = new StringBuilder();
            int pos = 0;
            while (m.find(pos)) {
                buff.append(s.substring(pos, m.start()));
                String sub = s.substring(m.start(), m.end());
                buff.append(handler.replace(sub));
                pos = m.end();
            }
            if (pos < s.length())
                buff.append(s.substring(pos));

            return buff.toString();
        } else {
            return s;
        }
    }

    public static String replaceRegex(String s, String regex, ReplaceHandler handler) {
        return replaceRegex(s, Pattern.compile(regex), handler);
    }

    public static interface ReplaceHandler {
        String replace(String replaced);
    }

    public static boolean isEnglishChar(char c) {
        return c > 0 && c < 128;
    }

    public static boolean isEnglishWord(String s) {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (!isEnglishChar(s.charAt(i)))
                return false;
        }
        return true;
    }

    public static boolean isChineseChar(char c) {
        return c >= 0x4E00 && c <= 0x9FA5;
    }

    public static boolean isChineseWord(String s) {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (!isChineseChar(s.charAt(i)))
                return false;
        }
        return true;
    }
}
