package com.borqs.server.base.util;


import com.borqs.server.base.util.json.JsonUtils;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TextCollection {
    private final BidiMap map = new DualHashBidiMap();


    public TextCollection() {
    }

    public TextCollection add(String text, int value) {
        Validate.notNull(text);
        map.put(text, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public Set<String> getTexts() {
        return (Set<String>)map.keySet();
    }

    @SuppressWarnings("unchecked")
    public Collection<Integer> getValues() {
        return (Collection<Integer>)map.values();
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

    public int[] getValuesArray() {
        Collection<Integer> values = getValues();
        if (values.isEmpty())
            return new int[0];

        return ArrayUtils.toPrimitive(values.toArray(new Integer[values.size()]));
    }

    public Integer getValue(String text) {
        Validate.notNull(text);
        return (Integer)map.get(text);
    }

    public int getValue(String text, int def) {
        Integer val = getValue(text);
        return val != null ? val : def;
    }

    public String getText(int value) {
        return (String)map.getKey(value);
    }

    public String getText(int value, String def) {
        String text = getText(value);
        return text != null ? text : def;
    }

    public int bitOr(String... texts) {
        Validate.notNull(texts);
        int r = 0;
        for (String text : texts) {
            Integer val = getValue(text);
            if (val == null)
                throw new IllegalArgumentException(String.format("Unknown '%s'", text));

            r |= val;
        }
        return r;
    }

    public int splitBitOr(String texts, String sep) {
        return bitOr(StringUtils2.splitArray(texts, sep, true));
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
        return JsonUtils.toJson(map, true);
    }

    public static TextCollection of(String t1, int v1) {
        TextCollection tc = new TextCollection();
        tc.add(t1, v1);
        return tc;
    }

    public static TextCollection of(String t1, int v1, String t2, int v2) {
        TextCollection tc = new TextCollection();
        tc.add(t1, v1);
        tc.add(t2, v2);
        return tc;
    }

    public static TextCollection of(String t1, int v1, String t2, int v2, String t3, int v3) {
        TextCollection tc = new TextCollection();
        tc.add(t1, v1);
        tc.add(t2, v2);
        tc.add(t3, v3);
        return tc;
    }

    public static TextCollection of(String t1, int v1, String t2, int v2, String t3, int v3, String t4, int v4) {
        TextCollection tc = new TextCollection();
        tc.add(t1, v1);
        tc.add(t2, v2);
        tc.add(t3, v3);
        tc.add(t4, v4);
        return tc;
    }

    public static TextCollection of(String t1, int v1, String t2, int v2, String t3, int v3, String t4, int v4, String t5, int v5) {
        TextCollection tc = new TextCollection();
        tc.add(t1, v1);
        tc.add(t2, v2);
        tc.add(t3, v3);
        tc.add(t4, v4);
        tc.add(t5, v5);
        return tc;
    }

    public static TextCollection of(String t1, int v1, String t2, int v2, String t3, int v3, String t4, int v4, String t5, int v5, String t6, int v6) {
        TextCollection tc = new TextCollection();
        tc.add(t1, v1);
        tc.add(t2, v2);
        tc.add(t3, v3);
        tc.add(t4, v4);
        tc.add(t5, v5);
        tc.add(t6, v6);
        return tc;
    }
    public static TextCollection of(String t1, int v1, String t2, int v2, String t3, int v3, String t4, int v4, String t5, int v5, String t6, int v6, String t7, int v7, String t8, int v8) {
        TextCollection tc = new TextCollection();
        tc.add(t1, v1);
        tc.add(t2, v2);
        tc.add(t3, v3);
        tc.add(t4, v4);
        tc.add(t5, v5);
        tc.add(t6, v6);
        tc.add(t7, v7);
        tc.add(t8, v8);
        return tc;
    }

    public static TextCollection of(String t1, int v1, String t2, int v2, String t3, int v3, String t4, int v4, String t5, int v5, String t6, int v6, String t7, int v7, String t8, int v8, String t9, int v9) {
        TextCollection tc = new TextCollection();
        tc.add(t1, v1);
        tc.add(t2, v2);
        tc.add(t3, v3);
        tc.add(t4, v4);
        tc.add(t5, v5);
        tc.add(t6, v6);
        tc.add(t7, v7);
        tc.add(t8, v8);
        tc.add(t9, v9);
        return tc;
    }

    public static TextCollection of(String t1, int v1, String t2, int v2, String t3, int v3, String t4, int v4, String t5, int v5, String t6, int v6, String t7, int v7, String t8, int v8, String t9, int v9, String t10, int v10) {
        TextCollection tc = new TextCollection();
        tc.add(t1, v1);
        tc.add(t2, v2);
        tc.add(t3, v3);
        tc.add(t4, v4);
        tc.add(t5, v5);
        tc.add(t6, v6);
        tc.add(t7, v7);
        tc.add(t8, v8);
        tc.add(t9, v9);
        tc.add(t10, v10);
        return tc;
    }
    public static TextCollection of(String t1, int v1, String t2, int v2, String t3, int v3, String t4, int v4, String t5, int v5, String t6, int v6, String t7, int v7, String t8, int v8, String t9, int v9, String t10, int v10, String t11, int v11) {
        TextCollection tc = new TextCollection();
        tc.add(t1, v1);
        tc.add(t2, v2);
        tc.add(t3, v3);
        tc.add(t4, v4);
        tc.add(t5, v5);
        tc.add(t6, v6);
        tc.add(t7, v7);
        tc.add(t8, v8);
        tc.add(t9, v9);
        tc.add(t10, v10);
        tc.add(t11, v11);
        return tc;
    }

    public static TextCollection of(String t1, int v1, String t2, int v2, String t3, int v3, String t4, int v4, String t5, int v5, String t6, int v6, 
              String t7, int v7, String t8, int v8, String t9, int v9, String t10, int v10, String t11, int v11, String t12, int v12) {
        TextCollection tc = new TextCollection();
        tc.add(t1, v1);
        tc.add(t2, v2);
        tc.add(t3, v3);
        tc.add(t4, v4);
        tc.add(t5, v5);
        tc.add(t6, v6);
        tc.add(t7, v7);
        tc.add(t8, v8);
        tc.add(t9, v9);
        tc.add(t10, v10);
        tc.add(t11, v11);
        tc.add(t12, v12);
        return tc;
    }
}
