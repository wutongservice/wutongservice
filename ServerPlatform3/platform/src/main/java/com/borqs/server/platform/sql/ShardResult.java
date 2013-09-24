package com.borqs.server.platform.sql;


import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShardResult {
    public final String db;
    public final String table;

    public ShardResult(String db, String table) {
        this.db = db;
        this.table = table;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ShardResult that = (ShardResult) o;
        return StringUtils.equals(db, that.db) && StringUtils.equals(table, that.table);
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.hashCode(db);
        result = 31 * result + ObjectUtils.hashCode(table);
        return result;
    }

    @Override
    public String toString() {
        return StringHelper.join(new String[]{table, "@", db});
    }

    public static String[] getDbs(List<ShardResult> srs) {
        ArrayList<String> dbs = new ArrayList<String>();
        if (srs != null) {
            for (ShardResult sr : srs)
                dbs.add(sr != null ? sr.db : null);
        }
        return dbs.toArray(new String[dbs.size()]);
    }

    public static String[] getDbs(ShardResult... srs) {
        return getDbs(Arrays.asList(srs));
    }
}
