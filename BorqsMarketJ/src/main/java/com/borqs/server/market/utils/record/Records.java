package com.borqs.server.market.utils.record;


import java.util.ArrayList;
import java.util.Collection;

public class Records extends ArrayList<Record> {
    public Records() {
    }

    public Records(int initialCapacity) {
        super(initialCapacity);
    }

    public Records(Collection<? extends Record> c) {
        super(c);
    }

    public void removeField(String field) {
        for (Record rec : this) {
            if (rec != null)
                rec.removeField(field);
        }
    }

    public void removeFields(String... fields) {
        for (Record rec : this) {
            if (rec != null)
                rec.removeFields(fields);
        }
    }


    public String[] valuesAsStringArray(String field) {
        ArrayList<String> vals = new ArrayList<String>();
        for (Record rec : this) {
            if (rec != null && rec.hasField(field))
                vals.add(rec.asString(field));
        }
        return vals.toArray(new String[vals.size()]);
    }


}
