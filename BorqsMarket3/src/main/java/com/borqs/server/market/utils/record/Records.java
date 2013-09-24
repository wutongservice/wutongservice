package com.borqs.server.market.utils.record;


import com.borqs.server.market.utils.CC;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class Records extends ArrayList<Record> {
    public Records() {
    }

    public Records(int initialCapacity) {
        super(initialCapacity);
    }

    public Records(Collection<? extends Record> c) {
        super(c);
    }

    public Records append(Record rec) {
        add(rec);
        return this;
    }

    public void addUnlessNull(Record rec) {
        if (rec != null)
            add(rec);
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


    public void renameField(String oldField, String newField) {
        for (Record rec : this) {
            if (rec != null)
                rec.renameField(oldField, newField);
        }
    }

    public void renameFields(Map<String, String> oldFieldsAndNewFields) {
        for (Record rec : this) {
            if (rec != null)
                rec.renameFields(oldFieldsAndNewFields);
        }
    }

    public void renameFields(String... oldFieldsAndNewFields) {
        renameFields(CC.strMap(oldFieldsAndNewFields));
    }

    public void retainsFields(String... fields) {
        for (Record rec : this) {
            if (rec != null)
                rec.retainsFields(fields);
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

    public void transactValue(String field, Record.ValueTransactor transactor) {
        for (Record rec : this) {
            if (rec != null)
                rec.transactValue(field, transactor);
        }
    }

    public Records aggregateMultipleLocale(String name) {
        for (Record rec : this) {
            if (rec != null)
                rec.aggregateMultipleLocale(name);
        }
        return this;
    }

    public Records aggregateMultipleLocale(String... names) {
        for (Record rec : this) {
            if (rec != null)
                rec.aggregateMultipleLocale(names);
        }
        return this;
    }

    public Records disperseMultipleLocale(String name) {
        for (Record rec : this) {
            if (rec != null)
                rec.disperseMultipleLocale(name);
        }
        return this;
    }

    public Records disperseMultipleLocale(String... names) {
        for (Record rec : this) {
            if (rec != null)
                rec.disperseMultipleLocale(names);
        }
        return this;
    }

//    public Records retain(String str) {
//        Records records = new Records();
//        if (StringUtils.isBlank(str))
//            return records;
//
//        for (Record rec : this) {
//            Record resultRec = new Record();
//            for (String s : str.split(",")) {
//                if (rec.containsKey(s))
//                    resultRec.put(s, rec.get(s));
//            }
//            records.add(resultRec);
//        }
//        return records;
//    }




    public String join(String column, String separator) {
        List<String> list = new ArrayList<String>();
        for (Record r : this) {
            list.add((String) r.get(column));
        }
        return StringUtils.join(list, separator);
    }

    public String[] asStringArray(String column) {
        List<String> l = asStringList(column);
        return l.toArray(new String[l.size()]);
    }

    public List<String> asStringList(String column) {
        ArrayList<String> list = new ArrayList<String>();
        for (Record r : this) {
            list.add((String) r.get(column));
        }
        return list;
    }

    public Set<String> asStringSet(String column) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        for (Record r : this) {
            set.add((String) r.get(column));
        }
        return set;
    }

    public Record findWithPredicate(Predicate pred) {
        for (Record rec : this) {
            if (pred.predicate(rec))
                return rec;
        }
        return null;
    }

    public boolean hasWithPredicate(Predicate pred) {
        for (Record rec : this) {
            if (pred.predicate(rec))
                return true;
        }
        return false;
    }

    public void sortAsString(final String field, final boolean asc) {
        Collections.sort(this, new Comparator<Record>() {
            @Override
            public int compare(Record o1, Record o2) {
                String v1 = o1.asString(field);
                String v2 = o2.asString(field);

                int n = ObjectUtils.compare(v1, v2);
                return asc ? n : -n;
            }
        });
    }

    public void sortAsLong(final String field, final boolean asc) {
        Collections.sort(this, new Comparator<Record>() {
            @Override
            public int compare(Record o1, Record o2) {
                long v1 = o1.asLong(field);
                long v2 = o2.asLong(field);
                int n = ObjectUtils.compare(v1, v2);
                return asc ? n : -n;
            }
        });
    }

    public Records setField(String field, Object val) {
        for (Record rec : this) {
            if (rec != null)
                rec.set(field, val);
        }
        return this;
    }

    public Records setFieldDefaultValue(String field, Object val) {
        for (Record rec : this) {
            if (rec != null && !rec.hasField(field))
                rec.set(field, val);
        }
        return this;
    }

    public static interface Predicate {
        boolean predicate(Record rec);
    }
}
