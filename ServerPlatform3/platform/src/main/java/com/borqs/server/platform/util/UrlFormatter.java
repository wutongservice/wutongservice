package com.borqs.server.platform.util;


import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.data.RecordSet;
import org.apache.commons.lang.StringUtils;

public class UrlFormatter {
    public static String format(String patt, String url) {
        return StringUtils.isNotBlank(url) ? String.format(patt, url) : "";
    }

    public static Record formatRecord(String patt, Record rec, String... cols) {
        for (String col : cols) {
            if (rec.has(col))
                rec.set(col, format(patt, rec.checkGetString(col)));
        }
        return rec;
    }

    public static RecordSet formatRecords(String patt, RecordSet recs, String... cols) {
        for (Record rec : recs)
            formatRecord(patt, rec, cols);
        return recs;
    }
}
