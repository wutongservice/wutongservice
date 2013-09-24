package com.borqs.server.platform.account2;


import com.borqs.server.platform.util.CollectionsHelper;

import java.util.*;

public class PropertyEntries extends ArrayList<PropertyEntry> {

    private static final Comparator<PropertyEntry> INDEX_COMPARATOR = new Comparator<PropertyEntry>() {
        @Override
        public int compare(PropertyEntry o1, PropertyEntry o2) {
            int i1 = o1.index;
            int i2 = o2.index;
            return i1 < i2 ? -1 : (i1 > i2 ? 1 : 0);
        }
    };

    public PropertyEntries() {
    }

    public PropertyEntries(int initialCapacity) {
        super(initialCapacity);
    }

    public PropertyEntries(Collection<? extends PropertyEntry> c) {
        super(c);
    }

    public void addEntry(int key, int sub, int index, Object value) {
        add(new PropertyEntry(key, sub, index, value, 0L));
    }

    public void addEntry(int key, int sub, int index, Object value, long updatedTime) {
        add(new PropertyEntry(key, sub, index, value, updatedTime));
    }

    public Map<Integer, PropertyEntry> getEntries(int key, int index) {
        LinkedHashMap<Integer, PropertyEntry> m = new LinkedHashMap<Integer, PropertyEntry>();
        for (PropertyEntry e : this) {
            if (e.key == key && e.index == index)
                m.put(e.sub, e);
        }
        return m;
    }

    public Object getSimple(int key) {
        for (PropertyEntry e : this) {
            if (e.key == key && e.sub == 0 && e.index <= 0)
                return e.value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List getSimpleArray(int key) {
        PropertyEntries l = new PropertyEntries();
        for (PropertyEntry e : this) {
            if (e.key == key && e.sub == 0)
                l.add(e);
        }
        Collections.sort(l, INDEX_COMPARATOR);
        ArrayList l0 = new ArrayList(size());
        for (PropertyEntry e : this)
            l0.add(e.value);
        return l0;
    }

    public Map<Integer, Object> getObject(int key) {
        Map<Integer, Object> m = new LinkedHashMap<Integer, Object>();
        for (PropertyEntry e : this) {
            if (e.key == key && e.index == 0)
                m.put(e.sub, e.value);
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    public Map<Integer, Object>[] getObjectArray(int key) {
        int maxIndex = -1;
        for (PropertyEntry e : this) {
            if (e.key == key && e.sub != 0)
                if (e.index > maxIndex)
                    maxIndex = e.index;
        }
        if (maxIndex > 999)
            maxIndex = 999;

        if (maxIndex < 0)
            return new Map[0];

        Map<Integer, Object>[] a = new Map[maxIndex + 1];
        for (int i = 0; i < a.length; i++)
            a[i] = new LinkedHashMap<Integer, Object>();

        for (PropertyEntry e : this) {
            if (e.key == key && e.sub != 0) {
                a[e.index].put(e.sub, e.value);
            }
        }
        return a;
    }

    public long getMaxUpdatedTime(int key) {
        long r = 0;
        for (PropertyEntry e : this) {
            if (e.key == key && e.updatedTime > r)
                r = e.updatedTime;
        }
        return r;
    }

    public int[] getKeys() {
        HashSet<Integer> keys = new HashSet<Integer>();
        for (PropertyEntry e : this)
            keys.add(e.key);

        return CollectionsHelper.toIntArray(keys);
    }

}
