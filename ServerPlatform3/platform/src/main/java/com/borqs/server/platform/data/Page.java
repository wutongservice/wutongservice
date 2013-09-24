package com.borqs.server.platform.data;


import com.borqs.server.platform.io.Writable;
import com.borqs.server.platform.util.CollectionsHelper;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.commons.lang.Validate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

public class Page implements Writable {
    public long page;
    public long count;

    public Page() {
        this(0, 0);
    }

    public Page(long page, long count) {
        this.page = page;
        this.count = count;
    }

    public static Page of(long page, long count) {
        return new Page(page, count);
    }

    public static Page of(long count) {
        return new Page(0, count);
    }

    public long getBegin() {
        return page * count;
    }

    public long getEnd() {
        return page * count + count;
    }

    public <T> void retains(List<T> l) {
        long cursor = getBegin();
        Validate.isTrue(cursor >= 0);
        if (cursor >= l.size() || count == 0) {
            l.clear();
        } else {
            ArrayList<T> ll = new ArrayList<T>();
            ListIterator<T> iter = l.listIterator((int) cursor);
            if (count > 0) {
                int n = 0;
                while (iter.hasNext() && n < count) {
                    ll.add(iter.next());
                    n++;
                }
            } else {
                while (iter.hasNext()) {
                    ll.add(iter.next());
                }
            }
            l.clear();
            l.addAll(ll);
        }
    }

    public <T> void retainsTo(List<T> l, List<T> result) {
        long cursor = getBegin();
        Validate.isTrue(cursor >= 0);
        if (cursor >= 0 && cursor < l.size()) {
            ListIterator<T> iter = l.listIterator((int) cursor);
            if (count > 0) {
                int n = 0;
                while (iter.hasNext() && n < count) {
                    result.add(iter.next());
                    n++;
                }
            } else {
                while (iter.hasNext()) {
                    result.add(iter.next());
                }
            }
        }
    }

    public long[] retains(long[] a) {
        ArrayList<Long> l = new ArrayList<Long>();
        retainsTo(CollectionsHelper.toLongList(a), l);
        return CollectionsHelper.toLongArray(l);
    }

    public String[] retains(String[] a) {
        ArrayList<String> l = new ArrayList<String>();
        retainsTo(Arrays.asList(a), l);
        return l.toArray(new String[l.size()]);
    }

    public boolean include(long index) {
        return index >= getBegin() && index < getEnd();
    }

    @Override
    public void write(Encoder out, boolean flush) throws IOException {
        out.writeLong(page);
        out.writeLong(count);
        if (flush)
            out.flush();
    }

    @Override
    public void readIn(Decoder in) throws IOException {
        page = in.readLong();
        count = in.readLong();
    }

    public static String toLimit(int page, int count) {
        if (page <= 0) {
            return count >= 0 ? "LIMIT " + count : "";
        } else {
            return String.format("LIMIT %s,%s", toOffset(page, count), count >= 0 ? count : Integer.MAX_VALUE);
        }
    }

    public static int toOffset(int page, int count) {
        return page * count;
    }
}
