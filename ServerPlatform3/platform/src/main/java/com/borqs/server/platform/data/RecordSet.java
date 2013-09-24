package com.borqs.server.platform.data;


import com.borqs.server.platform.io.IOHelper;
import com.borqs.server.platform.util.Copyable;
import com.borqs.server.platform.util.Predicate;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializableWithType;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.util.*;

public class RecordSet extends ArrayList<Record> implements Copyable<RecordSet>, JsonSerializableWithType {
    public RecordSet() {
    }

    public RecordSet(int initialCapacity) {
        super(initialCapacity);
    }

    public RecordSet(Collection<? extends Record> c) {
        addAll(c);
    }

    public static RecordSet newEmpty() {
        return new RecordSet();
    }

    @Override
    public boolean add(Record record) {
        Validate.notNull(record);
        return super.add(record);
    }

    @Override
    public void add(int index, Record element) {
        Validate.notNull(element);
        super.add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends Record> c) {
        Validate.notNull(c);
        for (Record rec : c)
            Validate.notNull(rec);

        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends Record> c) {
        Validate.notNull(c);
        for (Record rec : c)
            Validate.notNull(rec);

        return super.addAll(index, c);
    }

//    @Override
//    public void write(Encoder out, boolean flush) throws IOException {
//        out.writeArrayStart();
//        out.setItemCount(size());
//        for (Record rec : this) {
//            out.startItem();
//            rec.write(out, false);
//        }
//        out.writeArrayEnd();
//        if (flush)
//            out.flush();
//    }
//
//    @Override
//    public void readIn(Decoder in) throws IOException {
//        clear();
//
//        long l = in.readArrayStart();
//        if (l > 0) {
//            do {
//                for (long i = 0; i < l; i++) {
//                    Record rec = Record.read(in);
//                    add(rec);
//                }
//            } while ((l = in.arrayNext()) > 0);
//        }
//    }
//
//    public static RecordList read(Decoder in) throws IOException {
//        RecordList recs = new RecordList();
//        recs.readIn(in);
//        return recs;
//    }

    public List<Boolean> getBooleanColumnValues(String col) {
        ArrayList<Boolean> l = new ArrayList<Boolean>();
        for (Record rec : this)
            l.add(rec.getBoolean(col, false));
        return l;
    }

    public List<Long> getIntColumnValues(String col) {
        ArrayList<Long> l = new ArrayList<Long>();
        for (Record rec : this)
            l.add(rec.getInt(col));
        return l;
    }


    public List<Double> getFloatColumnValues(String col) {
        ArrayList<Double> l = new ArrayList<Double>();
        for (Record rec : this)
            l.add(rec.getFloat(col));
        return l;
    }

    public List<String> getStringColumnValues(String col) {
        ArrayList<String> l = new ArrayList<String>();
        for (Record rec : this)
            l.add(rec.getString(col));
        return l;
    }

    public RecordSet removeColumns(Collection<String> cols) {
        for (Record rec : this)
            rec.removeColumns(cols);
        return this;
    }

    public RecordSet removeColumns(String... cols) {
        return removeColumns(Arrays.asList(cols));
    }

    public RecordSet retainColumns(Collection<String> cols) {
        for (Record rec : this)
            rec.retainColumns(cols);
        return this;
    }

    public RecordSet retainColumns(String... cols) {
        return retainColumns(Arrays.asList(cols));
    }


    public RecordSet renameColumn(String oldCol, String newCol) {
        for (Record rec : this)
            rec.renameColumn(oldCol, newCol);
        return this;
    }

    public Record getFirstRecord() {
        return isEmpty() ? new Record() : get(0);
    }

    public String joinColumnValues(String col, String sep) {
        StringBuilder buff = new StringBuilder();
        int n = 0;
        for (Record rec : this) {
            if (rec.has(col)) {
                if (n > 0)
                    buff.append(",");
                buff.append(rec.getString(col, ""));
                n++;
            }
        }
        return buff.toString();
    }

    public Map<String, Record> toStringRecordMap(String col) {
        LinkedHashMap<String, Record> recm = new LinkedHashMap<String, Record>();
        for (Record rec : this) {
            String k = rec.getString(col, null);
            if (k != null)
                recm.put(k, rec);
        }
        return recm;
    }

    public Map<Long, Record> toIntRecordMap(String col) {
        LinkedHashMap<Long, Record> recm = new LinkedHashMap<Long, Record>();
        for (Record rec : this) {
            if (!rec.has(col))
                continue;

            long k = rec.getInt(col);
            recm.put(k, rec);
        }
        return recm;
    }

    public Map<Object, Record> toRecordMap(String col) {
        LinkedHashMap<Object, Record> recm = new LinkedHashMap<Object, Record>();
        for (Record rec : this) {
            if (!rec.has(col))
                continue;

            Object k = rec.get(col);
            recm.put(k, rec);
        }
        return recm;
    }

    public Map<String, Record> toJoinedStringRecordMap(String sep, String... cols) {
        Validate.notEmpty(cols);
        LinkedHashMap<String, Record> recm = new LinkedHashMap<String, Record>();
        for (Record rec : this) {
            StringBuilder k = new StringBuilder();
            for (int i = 0; i < cols.length; i++) {
                String col = cols[i];
                String s = rec.getString(col, null);
                if (i > 0)
                    k.append(sep);
                k.append(s != null ? s : "");
            }
            recm.put(k.toString(), rec);
        }
        return recm;
    }

    public Map<String, RecordSet> groupByStringColumn(String col) {
        LinkedHashMap<String, RecordSet> grouped = new LinkedHashMap<String, RecordSet>();
        for (Record rec : this) {
            String k = rec.checkGetString(col);
            RecordSet s = grouped.get(k);
            if (s == null) {
                s = new RecordSet();
                grouped.put(k, s);
            }
            s.add(rec);
        }
        return grouped;
    }

    public Map<Long, RecordSet> groupByIntColumn(String col) {
        LinkedHashMap<Long, RecordSet> grouped = new LinkedHashMap<Long, RecordSet>();
        for (Record rec : this) {
            Long k = rec.checkGetInt(col);
            RecordSet s = grouped.get(k);
            if (s == null) {
                s = new RecordSet();
                grouped.put(k, s);
            }
            s.add(rec);
        }
        return grouped;
    }

    public Record findFirst(String col, Object v) {
        for (Record rec : this) {
            Object o = rec.get(col);
            if (o != null && o.equals(v))
                return rec;
        }
        return null;
    }


    @Override
    public void serializeWithType(JsonGenerator jsonGenerator, SerializerProvider serializerProvider, TypeSerializer typeSerializer) throws IOException, JsonProcessingException {
        jsonWrite(jsonGenerator, false);
    }

    @Override
    public void serialize(JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        jsonWrite(jsonGenerator, false);
    }

    public void jsonWrite(JsonGenerator jg, boolean ignoreNull) throws IOException {
        jg.writeStartArray();
        for (Record rec : this) {
            rec.jsonWrite(jg, ignoreNull);
        }
        jg.writeEndArray();
    }

    @Override
    public String toString() {
        return toJson();
    }

    public String toJson() {
        return toJson(false, true);
    }

    public String toJson(final boolean ignoreNull, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                jsonWrite(jg, ignoreNull);
            }
        }, human);
    }

    public JsonNode toJsonNode() {
        return JsonHelper.parse(toJson(false, false));
    }

    public static RecordSet fromJson(String json) {
        return JsonHelper.fromJson(json, RecordSet.class);
    }

    public static RecordSet fromJsonNode(JsonNode jn) {
        return JsonHelper.fromJsonNode(jn, RecordSet.class);
    }

    @Override
    public RecordSet copy() {
        RecordSet recs = new RecordSet();
        recs.addAll(this);
        return recs;
    }

    public RecordSet filterCopy(Predicate<Record> pred) {
        RecordSet recs = new RecordSet();
        for (Record rec : this) {
            if (pred != null && pred.predicate(rec))
                recs.add(rec);
        }
        return recs;
    }

    public RecordSet filter(Predicate<Record> pred) {
        RecordSet recs = filterCopy(pred);
        clear();
        addAll(recs);
        return this;
    }

    public RecordSet sortCopy(String col, boolean asc) {
        return copy().sort(col, asc);
    }

    public RecordSet sort(final String col, final boolean asc) {
        Validate.notNull(col);
        Collections.sort(this, new Comparator<Record>() {
            @Override
            public int compare(Record rec1, Record rec2) {
                Object o1 = rec1.get(col);
                Object o2 = rec2.get(col);

                Comparable c1 = o1 instanceof Comparable ? (Comparable) o1 : (o1 != null ? o1.toString() : null);
                Comparable c2 = o2 instanceof Comparable ? (Comparable) o2 : (o2 != null ? o2.toString() : null);

                return asc ? ObjectUtils.compare(c1, c2) : ObjectUtils.compare(c2, c1);
            }
        });
        return this;
    }

    public RecordSet sortInCopy(String col, List order) {
        Map<Object, Record> recm = toRecordMap(col);
        RecordSet recs = new RecordSet();
        for (Object k : order) {
            Record rec = recm.get(k);
            if (rec != null)
                recs.add(rec);
        }
        return recs;
    }

    public RecordSet sortIn(String col, List order) {
        RecordSet recs = sortInCopy(col, order);
        clear();
        addAll(recs);
        return this;
    }

    public RecordSet sliceCopy(long cursor, long count) {
        Validate.isTrue(cursor >= 0);
        if (cursor >= size() || count == 0)
            return new RecordSet();

        RecordSet recs = new RecordSet();
        ListIterator<Record> iter = listIterator((int) cursor);
        if (count > 0) {
            int n = 0;
            while (iter.hasNext() && n < count) {
                recs.add(iter.next());
                n++;
            }
        } else {
            while (iter.hasNext()) {
                recs.add(iter.next());
            }
        }
        return recs;
    }

    public RecordSet slice(long cursor, long count) {
        RecordSet recs = sliceCopy(cursor, count);
        clear();
        addAll(recs);
        return this;
    }

    public RecordSet sliceByPageCopy(Page page) {
        return page != null ? sliceCopy(page.getBegin(), page.count) : copy();
    }

    public RecordSet sliceByPage(Page page) {
        return page != null ? slice(page.page * page.count, page.count) : this;
    }

    public RecordSet shuffleCopy() {
        return copy().shuffle();
    }

    public RecordSet shuffle() {
        Collections.shuffle(this);
        return this;
    }

    public RecordSet uniqueCopy() {
        LinkedHashSet<Record> recs = new LinkedHashSet<Record>(this);
        return new RecordSet(recs);
    }

    public RecordSet unique() {
        LinkedHashSet<Record> recs = new LinkedHashSet<Record>(this);
        clear();
        addAll(recs);
        return this;
    }

    public RecordSet uniqueCopy(String col) {
        LinkedHashMap<Object, Record> recm = new LinkedHashMap<Object, Record>();
        for (Record rec : this)
            recm.put(rec.checkGet(col), rec);
        return new RecordSet(recm.values());
    }

    public RecordSet unique(String col) {
        LinkedHashMap<Object, Record> recm = new LinkedHashMap<Object, Record>();
        for (Record rec : this)
            recm.put(rec.checkGet(col), rec);

        clear();
        addAll(recm.values());
        return this;
    }

    public void foreach(Handler<Record> handler) {
        foreach(handler, false);
    }

    public void foreach(Handler<Record> handler, boolean errorResume) {
        Validate.notNull(handler);
        if (errorResume) {
            for (Record rec : this) {
                try {
                    handler.handle(rec);
                } catch (Throwable ignored) {
                }
            }
        } else {
            for (Record rec : this)
                handler.handle(rec);
        }
    }

    public void foreachIf(Predicate<Record> pred, Handler<Record> action) {
        foreachIf(pred, action, false);
    }

    public void foreachIf(Predicate<Record> pred, Handler<Record> action, boolean errorResume) {
        Validate.notNull(pred);
        Validate.notNull(action);
        if (errorResume) {
            for (Record rec : this) {
                try {
                    if (pred.predicate(rec))
                        action.handle(rec);
                } catch (Throwable ignored) {
                }
            }
        } else {
            for (Record rec : this) {
                if (pred.predicate(rec))
                    action.handle(rec);
            }
        }
    }

    public long sumInt(String col) {
        long sum = 0;
        for (Record rec : this) {
            sum += rec.getInt(col, 0);
        }
        return sum;
    }

    public double sumFloat(String col) {
        double sum = 0;
        for (Record rec : this) {
            sum += rec.getFloat(col, 0.0);
        }
        return sum;
    }

    public double average(String col) {
        if (isEmpty())
            return 0.0;

        double sum = 0;
        for (Record rec : this) {
            sum += rec.getFloat(col, 0.0);
        }
        return sum / size();
    }

    public RecordSet mergeByKeys(String keyCol, RecordSet other, String otherKeyCol, Record def) {
        Map<String, Record> otherMap = other.toStringRecordMap(otherKeyCol);
        for (Record rec : this) {
            String key = rec.checkGetString(keyCol);
            Record otherRec = otherMap.get(key);
            if (otherRec != null) {
                rec.putAll(otherRec);
            } else {
                if (def != null)
                    rec.putAll(def);
            }
        }
        return this;
    }

    public RecordSet mergeJsonByKeys(String keyCol, RecordSet other, String otherKeyCol, Record def) {
        Map<String, Record> otherMap = other.toStringRecordMap(otherKeyCol);
        for (Record rec : this) {
            String key = rec.checkGetString(keyCol);
            Record otherRec = otherMap.get(key);
            if (otherRec != null) {
                rec.put(keyCol, otherRec.toJsonNode());
            } else {
                if (def != null)
                    rec.put(keyCol, otherRec.toJsonNode());
            }
        }
        return this;
    }

    public static RecordSet of(Record rec) {
        RecordSet recs = new RecordSet();
        recs.add(rec);
        return recs;
    }

    public static RecordSet of(Record... recArray) {
        RecordSet recs = new RecordSet();
        Collections.addAll(recs, recArray);
        return recs;
    }


    public byte[] toBytes() {
        return IOHelper.toBytes(this);
    }

    public static RecordSet fromBytes(byte[] bytes, int off, int len) {
        return (RecordSet) IOHelper.fromBytes(bytes, off, len);
    }

    public static RecordSet fromBytes(byte[] bytes) {
        return (RecordSet) IOHelper.fromBytes(bytes);
    }
}
