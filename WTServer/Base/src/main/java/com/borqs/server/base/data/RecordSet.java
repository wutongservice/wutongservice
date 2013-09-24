package com.borqs.server.base.data;


import com.borqs.server.base.io.IOUtils2;
import com.borqs.server.base.io.Serializable;
import com.borqs.server.base.util.CollectionUtils2;
import com.borqs.server.base.util.Copyable;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.util.json.JsonGenerateHandler;
import com.borqs.server.base.util.json.JsonUtils;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializableWithType;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class RecordSet extends ArrayList<Record> implements Copyable<RecordSet>, Serializable, JsonSerializableWithType {
    public RecordSet() {
    }

    public RecordSet(int initialCapacity) {
        super(initialCapacity);
    }

    public RecordSet(Collection<? extends Record> c) {
        addAll(c);
    }

    public RecordSet(Record... recs) {
        if (ArrayUtils.isNotEmpty(recs))
            Collections.addAll(this, recs);
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

    @Override
    public void write(Encoder out, boolean flush) throws IOException {
        out.writeArrayStart();
        out.setItemCount(size());
        for (Record rec : this) {
            out.startItem();
            rec.write(out, false);
        }
        out.writeArrayEnd();
        if (flush)
            out.flush();
    }

    @Override
    public void readIn(Decoder in) throws IOException {
        clear();

        long l = in.readArrayStart();
        if (l > 0) {
            do {
                for (long i = 0; i < l; i++) {
                    Record rec = Record.read(in);
                    add(rec);
                }
            } while ((l = in.arrayNext()) > 0);
        }
    }

    public static RecordSet read(Decoder in) throws IOException {
        RecordSet recs = new RecordSet();
        recs.readIn(in);
        return recs;
    }

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

    public long[] getIntColumnValuesAsArray(String col) {
        return CollectionUtils2.toLongArray(getIntColumnValues(col));
    }

    public List<String> getStringColumnValues(String col) {
        ArrayList<String> l = new ArrayList<String>();
        for (Record rec : this)
            l.add(rec.getString(col));
        return l;
    }

    public String[] getStringColumnValuesAsArray(String col) {
        List<String> l = getStringColumnValues(col);
        return l.toArray(new String[l.size()]);
    }

    public List<Long> getIntColumnValues(String col, String sep) {
        ArrayList<Long> l = new ArrayList<Long>();
        for (Record rec : this) {
            Object v = rec.get(col);
            if (v instanceof Long) {
                l.add((Long) v);
            } else {
                l.addAll(StringUtils2.splitIntList(Values.toString(v), sep));
            }
        }
        return l;
    }

    public long[] getIntColumnValuesAsArray(String col, String sep) {
        return CollectionUtils2.toLongArray(getIntColumnValues(col, sep));
    }

    public List<String> getStringColumnValues(String col, String sep, boolean strip) {
        ArrayList<String> l = new ArrayList<String>();
        for (Record rec : this) {
            String s = rec.getString(col, "");
            l.addAll(StringUtils2.splitList(s, sep, strip));
        }
        return l;
    }

    public String[] getStringColumnValuesAsArray(String col, String sep, boolean strip) {
        List<String> l = getStringColumnValues(col, sep, strip);
        return l.toArray(new String[l.size()]);
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

    public void addIf(boolean b, Record rec) {
        if (b)
            add(rec);
    }

    public RecordSet renameColumn(String oldCol, String newCol) {
        for (Record rec : this)
            rec.renameColumn(oldCol, newCol);
        return this;
    }

    public RecordSet copyColumn(String col, String newCol) {
        for (Record rec : this) {
            rec.copyColumn(col, newCol);
        }
        return this;
    }

    public Record getFirstRecord() {
        return isEmpty() ? new Record() : get(0);
    }

    public String joinColumnValues(String col, String sep) {
        StringBuilder buff = new StringBuilder();
        int n = 0;
        for (Record rec : this) {
            if (rec.containsKey(col)) {
                if (n > 0)
                    buff.append(",");
                buff.append(rec.getString(col, ""));
                n++;
            }
        }
        return buff.toString();
    }

    public Map<String, Record> toRecordMap(String col) {
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
            if (rec.has(col)) {
                long k = rec.getInt(col);
                recm.put(k, rec);
            }
        }
        return recm;
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
        return toString(false, true);
    }

    public String toString(final boolean ignoreNull, boolean human) {
        return JsonUtils.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg) throws IOException {
                jsonWrite(jg, ignoreNull);
            }
        }, human);
    }

    public JsonNode toJsonNode() {
        return JsonUtils.parse(toString(false, false));
    }

    public static RecordSet fromJson(String json) {
        return JsonUtils.fromJson(json, RecordSet.class);
    }

    @Override
    public RecordSet copy() {
        RecordSet recs = new RecordSet();
        recs.addAll(this);
        return recs;
    }

    public RecordSet sortCopy(String col, boolean asc) {
        return copy().sort(col, asc);
    }

    public RecordSet sort(String col, boolean asc) {
        Validate.notNull(col);
        final String col0 = col;
        final boolean asc0 = asc;
        Collections.sort(this, new Comparator<Record>() {
            @Override
            public int compare(Record rec1, Record rec2) {
                Object o1 = rec1.get(col0);
                Object o2 = rec2.get(col0);

                Comparable c1 = o1 instanceof Comparable ? (Comparable)o1 : (o1 != null ? o1.toString() : null);
                Comparable c2 = o2 instanceof Comparable ? (Comparable)o2 : (o2 != null ? o2.toString() : null);

                return asc0 ? ObjectUtils.compare(c1, c2) : ObjectUtils.compare(c2, c1);
            }
        });
        return this;
    }

    public RecordSet sliceCopy(int cursor, int count) {
        Validate.isTrue(cursor >= 0);
        if (cursor >= size() || count == 0)
            return new RecordSet();

        RecordSet recs = new RecordSet();
        ListIterator<Record> iter = listIterator(cursor);
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

    public RecordSet slice(int cursor, int count) {
        RecordSet recs = sliceCopy(cursor, count);
        clear();
        addAll(recs);
        return this;
    }

    public RecordSet sliceByPageCopy(int page, int count) {
        return sliceCopy(page * count, count);
    }

    public RecordSet sliceByPage(int page, int count) {
        return slice(page * count, count);
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

    public void foreach(RecordHandler handler) {
        foreach(handler, false);
    }

    public void foreach(RecordHandler handler, boolean errorResume) {
        Validate.notNull(handler);
        if (errorResume) {
            for (Record rec : this) {
                try {
                    handler.handle(rec);
                } catch (Throwable ignored) {
                }
            }
        } else {
            for (Record rec : this) {
                handler.handle(rec);
            }
        }
    }

    public void foreachIf(RecordPredicate pred, RecordHandler action) {
        foreachIf(pred, action, false);
    }

    public void foreachIf(RecordPredicate pred, RecordHandler action, boolean errorResume) {
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
        Map<String, Record> otherMap = other.toRecordMap(otherKeyCol);
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
        Map<String, Record> otherMap = other.toRecordMap(otherKeyCol);
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

    public void retainsCount(int size) {
        int len = size();
        if (len > size)
            removeRange(size - 1, len - 1);
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
        return IOUtils2.toBytes(this);
    }

    public ByteBuffer toByteBuffer() {
        return IOUtils2.toByteBuffer(this);
    }

    public static RecordSet fromByteBuffer(ByteBuffer byteBuff) {
        return IOUtils2.fromByteBuffer(RecordSet.class, byteBuff);
    }

    public static RecordSet fromBytes(byte[] bytes, int off, int len) {
        return IOUtils2.fromBytes(RecordSet.class, bytes, off, len);
    }

    public static RecordSet fromBytes(byte[] bytes) {
        return IOUtils2.fromBytes(RecordSet.class, bytes);
    }
}
