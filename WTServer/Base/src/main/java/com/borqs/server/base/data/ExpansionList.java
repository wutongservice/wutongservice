package com.borqs.server.base.data;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.util.ClassUtils2;
import com.borqs.server.base.util.StringUtils2;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;

public class ExpansionList extends ArrayList<Expansion> {
    public ExpansionList(int initialCapacity) {
        super(initialCapacity);
    }

    public ExpansionList() {
    }

    public ExpansionList(Collection<? extends Expansion> c) {
        super(c);
    }

    public void expand(Context ctx, RecordSet recs, String[] cols) {
        for (Expansion exp : this) {
            if (exp != null)
                exp.expand(ctx, recs, cols);
        }
    }

    public void load(String classNames) {
        if (StringUtils.isNotEmpty(classNames)) {
            for (String className : StringUtils2.splitArray(classNames, ",", true)) {
                add((Expansion) ClassUtils2.newInstance(className));
            }
        }
    }

    public static ExpansionList loadNew(String classNames) {
        ExpansionList l = new ExpansionList();
        l.load(classNames);
        return l;
    }
}
