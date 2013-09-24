package com.borqs.server.platform.fts;


import com.borqs.server.platform.util.CollectionsHelper;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class FTResult extends ArrayList<FTResult.Entry> {

    public FTResult() {
    }

    public FTResult(Collection<? extends Entry> c) {
        super(c);
    }

    public FTResult addEntry(String category, String id, double weight, String field, String content) {
        Entry entry = null;
        for (Entry e0 : this) {
            if (e0 != null && StringUtils.equals(category, e0.category) && StringUtils.equals(id, e0.id)) {
                entry = e0;
                break;
            }
        }
        if (entry == null) {
            HashMap<String, String> hit = new HashMap<String, String>();
            hit.put(field, content);
            entry = new Entry(category, id, weight, hit);
            add(entry);
        } else {
            entry.hitContents.put(field, content);
        }
        return this;
    }

    public String[] getDocIds() {
        LinkedHashSet<String> ids = new LinkedHashSet<String>();
        for (Entry entry : this) {
            if (entry != null) {
                String id = entry.id;
                if (id != null)
                    ids.add(id);
            }

        }
        return ids.toArray(new String[ids.size()]);
    }

    public long[] getDocIdsAsLong() {
        LinkedHashSet<Long> ids = new LinkedHashSet<Long>();
        for (Entry entry : this) {
            if (entry != null) {
                String id = entry.id;
                if (id != null)
                    ids.add(Long.parseLong(id));
            }

        }
        return CollectionsHelper.toLongArray(ids);
    }

    public void sortByWeight(final boolean asc) {
        Collections.sort(this, new Comparator<Entry>() {
            @Override
            public int compare(Entry o1, Entry o2) {
                int r = Double.compare(o1.weight, o2.weight);
                if (!asc)
                    r = -r;
                return r;
            }
        });
    }

    public static class Entry {
        public final String category;
        public final String id;
        public double weight;
        public final Map<String, String> hitContents;

        public Entry(String category, String id, double wight, Map<String, String> hitContents) {
            this.category = category;
            this.id = id;
            this.weight = wight;
            this.hitContents = hitContents;
        }
    }
}
