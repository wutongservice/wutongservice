package com.borqs.server.base.data;


import org.apache.commons.lang.ObjectUtils;

public class RecordPredicates {

    public static RecordPredicate valueEquals(String col, Object val) {
        return new EqualsPredicate(col, val);
    }

    private static class EqualsPredicate implements RecordPredicate {
        final String column;
        final Object value;

        public EqualsPredicate(String column, Object value) {
            this.column = column;
            this.value = value;
        }

        @Override
        public boolean predicate(Record rec) {
            return ObjectUtils.equals(rec.get(column), value);
        }
    }
}
