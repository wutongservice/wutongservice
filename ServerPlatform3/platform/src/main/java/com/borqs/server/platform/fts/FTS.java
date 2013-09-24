package com.borqs.server.platform.fts;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.util.ArrayHelper;


public interface FTS {
    void saveDoc(Context ctx, FTDoc... docs);
    void deleteDoc(Context ctx, FTDoc... docs);
    void addFields(Context ctx, FTDoc... docs);
    void removeFields(Context ctx, FTDoc... docs);
    void updateFields(Context ctx, FTDoc... docs);
    FTResult search(Context ctx, FTResult reuse, String category, String word, Options opts, int count);

    public static class Options extends Record {
        public static final String IN_FIELDS = "in_fields";
        public static final String IN_IDS = "in_ids";
        public static final String METHOD = "method";
        public static final String INCR_WEIGHT = "incr_weight";

        public static final int METHOD_EQUALS = 1;
        public static final int METHOD_LIKE = 2;
        public static final int METHOD_FT_MATCH = 3;


        public String[] getInFields() {
            String[] inFields = (String[])get(IN_FIELDS);
            return inFields == null ? new String[0] : inFields;
        }

        public Options setInFields(String... fields) {
            put(IN_FIELDS, fields);
            return this;
        }

        public String[] getInIds() {
            String[] inIds = (String[])get(IN_IDS);
            return inIds == null ? new String[0] : inIds;
        }

        public Options setInIds(String... ids) {
            put(IN_IDS, ids);
            return this;
        }

        public Options setInIds(long... ids) {
            return setInIds(ArrayHelper.longArrayToStringArray(ids));
        }

        public int getMethod() {
            return (int)getInt(METHOD, METHOD_FT_MATCH);
        }

        public Options setMethod(int method) {
            set(METHOD, (long)method);
            return this;
        }

        public double getIncrWeight() {
            return getFloat(INCR_WEIGHT, 0.0);
        }

        public Options setIncrWeight(double incrWeight) {
            set(INCR_WEIGHT, incrWeight);
            return this;
        }
    }
}
