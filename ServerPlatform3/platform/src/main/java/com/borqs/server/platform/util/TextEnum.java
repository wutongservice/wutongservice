package com.borqs.server.platform.util;


import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TextEnum {
    private final BidiMap map = new DualHashBidiMap();


    public TextEnum() {
    }

    public TextEnum add(String text, int value) {
        Validate.notNull(text);
        map.put(text, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public Set<String> getTexts() {
        return (Set<String>) map.keySet();
    }

    @SuppressWarnings("unchecked")
    public Collection<Integer> getValues() {
        return (Collection<Integer>) map.values();
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public void removeText(String text) {
        map.remove(text);
    }

    public void removeValue(String value) {
        map.removeValue(value);
    }

    public void clear() {
        map.clear();
    }

    public boolean hasText(String text) {
        return map.containsKey(text);
    }

    public boolean hasValue(int value) {
        return map.containsValue(value);
    }

    public int parse(String s) {
        s = StringUtils.trimToEmpty(s);
        if (StringUtils.isNumeric(s)) {
            return Integer.parseInt(s);
        } else {
            Integer v = getValue(s);
            if (v == null)
                throw new IllegalArgumentException(String.format("Unknown '%s'", s));
            return v;
        }
    }

    public int strictParse(String s, int def) {
        s = StringUtils.trimToEmpty(s);
        if (StringUtils.isNumeric(s)) {
            int v = Integer.parseInt(s);
            return hasValue(v) ? v : def;
        } else {
            Integer v = getValue(s);
            return v != null ? v : def;
        }
    }

    public int checkStrictParse(String s) {
        s = StringUtils.trimToEmpty(s);
        if (StringUtils.isNumeric(s)) {
            int v = Integer.parseInt(s);
            if (!hasValue(v))
                throw new IllegalArgumentException(String.format("Unknown '%s'", s));
            return v;
        } else {
            Integer v = getValue(s);
            if (v == null)
                throw new IllegalArgumentException(String.format("Unknown '%s'", s));
            return v;
        }
    }

    public int[] getValuesArray() {
        Collection<Integer> values = getValues();
        if (values.isEmpty())
            return new int[0];

        return ArrayUtils.toPrimitive(values.toArray(new Integer[values.size()]));
    }

    public Integer getValue(String text) {
        Validate.notNull(text);
        return (Integer) map.get(text);
    }

    public int getValue(String text, int def) {
        Integer val = getValue(text);
        return val != null ? val : def;
    }

    public String getText(int value) {
        return (String) map.getKey(value);
    }

    public String getText(int value, String def) {
        String text = getText(value);
        return text != null ? text : def;
    }

    public int bitOr(String... texts) {
        Validate.notNull(texts);
        int r = 0;
        for (String text : texts) {
            int val = checkStrictParse(text);
            r |= val;
        }
        return r;
    }

    public int splitBitOr(String texts, String sep) {
        return bitOr(StringHelper.splitArray(texts, sep, true));
    }

    public Set<String> bitAnd(int values) {
        HashSet<String> set = new HashSet<String>();
        for (int val : getValuesArray()) {
            if ((val & values) != 0)
                set.add(getText(val));
        }
        return set;
    }

    public String bitAndJoin(int values, String sep) {
        if (sep == null)
            sep = "";
        return StringUtils.join(bitAnd(values), sep);
    }

    @Override
    public String toString() {
        return JsonHelper.toJson(map, true);
    }

    public static TextEnum of(String t1, int v1) {
        TextEnum tc = new TextEnum();
        tc.add(t1, v1);
        return tc;
    }

    public static TextEnum of(String t1, int v1, String t2, int v2) {
        TextEnum tc = new TextEnum();
        tc.add(t1, v1);
        tc.add(t2, v2);
        return tc;
    }

    public static TextEnum of(String t1, int v1, String t2, int v2, String t3, int v3) {
        TextEnum tc = new TextEnum();
        tc.add(t1, v1);
        tc.add(t2, v2);
        tc.add(t3, v3);
        return tc;
    }

    public static TextEnum of(String t1, int v1, String t2, int v2, String t3, int v3, String t4, int v4) {
        TextEnum tc = new TextEnum();
        tc.add(t1, v1);
        tc.add(t2, v2);
        tc.add(t3, v3);
        tc.add(t4, v4);
        return tc;
    }

    public static TextEnum of(String t1, int v1, String t2, int v2, String t3, int v3, String t4, int v4, String t5, int v5) {
        TextEnum tc = new TextEnum();
        tc.add(t1, v1);
        tc.add(t2, v2);
        tc.add(t3, v3);
        tc.add(t4, v4);
        tc.add(t5, v5);
        return tc;
    }

    public static TextEnum of(String t1, int v1, String t2, int v2, String t3, int v3, String t4, int v4, String t5, int v5, String t6, int v6) {
        TextEnum tc = new TextEnum();
        tc.add(t1, v1);
        tc.add(t2, v2);
        tc.add(t3, v3);
        tc.add(t4, v4);
        tc.add(t5, v5);
        tc.add(t6, v6);
        return tc;
    }

    public static TextEnum of(Object[][] arr) {
        TextEnum tc = new TextEnum();
        for (Map.Entry<String, Object> e : CollectionsHelper.arraysToMap(arr).entrySet())
            tc.add(e.getKey(), ((Number) e.getValue()).intValue());

        return tc;
    }
}
