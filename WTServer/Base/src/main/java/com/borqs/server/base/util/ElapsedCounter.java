package com.borqs.server.base.util;


import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ElapsedCounter {
    private long start;
    private List<RecordItem> records = new ArrayList<RecordItem>();

    public ElapsedCounter() {
        start = System.currentTimeMillis();
    }

    public void resetStart() {
        start = System.currentTimeMillis();
        records.add(null);
    }

    public long getStart() {
        return start;
    }

    public List<RecordItem> getRecords() {
        return records;
    }

    public void record(String tag) {
        records.add(new RecordItem(ObjectUtils.toString(tag), System.currentTimeMillis() - start));
    }

    public void clearRecords() {
        records.clear();
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

    public String[] toStringArray() {
        ArrayList<String> l = new ArrayList<String>();
        RecordItem prev = null;
        for (RecordItem rec : records) {
            if (rec != null) {
                if (prev == null) {
                    l.add(rec.tag + ": " + rec.elapsed);
                } else {
                    long delta = rec.elapsed - prev.elapsed;
                    l.add(rec.tag + ": " + rec.elapsed + " +" + delta);
                }
            } else {
                l.add("--- Reset ---");
            }
            prev = rec;
        }
        return l.toArray(new String[l.size()]);
    }

    @Override
    public String toString() {
        return StringUtils.join(toStringArray(), '\n');
    }

    public static class RecordItem {
        public String tag;
        public long elapsed;

        public RecordItem(String tag, long elapsed) {
            this.tag = tag;
            this.elapsed = elapsed;
        }
    }
    public long getTotalTime(){
        RecordItem riStart = records.get(0);
        RecordItem riEnd = records.get(records.size()-1);
        long result = riEnd.elapsed -riStart.elapsed;
        return result;
    }
}
