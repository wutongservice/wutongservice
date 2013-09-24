package com.borqs.server.wutong.account2.util;


import com.borqs.server.base.util.CollectionUtils2;
import com.borqs.server.base.util.StringUtils2;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class ColumnsExpander {
    public static String[] expand(String[] cols, Map<String, String[]> aliases) {
        HashMap<String, String[]> m = new HashMap<String, String[]>();
        for (Map.Entry<String, String[]> e : aliases.entrySet())
            splitPut(m, e.getKey(), e.getValue());

        LinkedHashSet<String> l = new LinkedHashSet<String>();
        for (String col : cols) {
            String[] v = m.get(col);
            if (v != null)
                Collections.addAll(l, v);
            else
                l.add(col);
        }
        return l.toArray(new String[l.size()]);
    }

    private static void splitPut(Map<String, String[]> m, String alias, String[] cols) {
        for (String a0 : StringUtils2.splitList(alias, ",", true))
            m.put(a0, cols);
    }

    public static String[] expand(String[] cols,
                                  String alias1, String[] cols1) {
        return expand(cols, CollectionUtils2.of(alias1, cols1));
    }

    public static String[] expand(String[] cols,
                                  String alias1, String[] cols1,
                                  String alias2, String[] cols2) {
        return expand(cols, CollectionUtils2.of(alias1, cols1, alias2, cols2));
    }

    public static String[] expand(String[] cols,
                                  String alias1, String[] cols1,
                                  String alias2, String[] cols2,
                                  String alias3, String[] cols3) {
        return expand(cols, CollectionUtils2.of(alias1, cols1, alias2, cols2, alias3, cols3));
    }

    public static String[] expand(String[] cols,
                                  String alias1, String[] cols1,
                                  String alias2, String[] cols2,
                                  String alias3, String[] cols3,
                                  String alias4, String[] cols4) {
        return expand(cols, CollectionUtils2.of(alias1, cols1, alias2, cols2, alias3, cols3, alias4, cols4));
    }

    public static String[] expand(String[] cols,
                                  String alias1, String[] cols1,
                                  String alias2, String[] cols2,
                                  String alias3, String[] cols3,
                                  String alias4, String[] cols4,
                                  String alias5, String[] cols5) {
        return expand(cols, CollectionUtils2.of(alias1, cols1, alias2, cols2, alias3, cols3, alias4, cols4, alias5, cols5));
    }
}
