package com.borqs.server.platform.io;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.data.RecordSet;
import com.borqs.server.platform.util.ClassHelper;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.codehaus.jackson.JsonNode;

import java.io.*;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;

public class RW {

    // Common
    public static final int NULL = 0;
    public static final int WRITABLE = 1;
    public static final int SERIALIZABLE = 2;

    // Primitive
    public static final int BOOLEAN = 11;
    public static final int BYTE = 12;
    public static final int SHORT = 13;
    public static final int INT = 14;
    public static final int LONG = 15;
    public static final int FLOAT = 16;
    public static final int DOUBLE = 17;
    public static final int CHAR = 18;
    public static final int STRING = 19;

    // Ext
    //public static final int BIG_DECIMAL = 20;
    public static final int BIG_INTEGER = 21;
    public static final int DATE = 23;
    public static final int JSON = 24;
    public static final int BYTE_BUFFER = 25;


    // Array
    public static final int OBJECT_ARRAY = 30;
    public static final int BOOLEAN_ARRAY = 31;
    public static final int BYTE_ARRAY = 32;
    public static final int SHORT_ARRAY = 33;
    public static final int INT_ARRAY = 34;
    public static final int LONG_ARRAY = 35;
    public static final int FLOAT_ARRAY = 36;
    public static final int DOUBLE_ARRAY = 37;
    public static final int CHAR_ARRAY = 38;

    // List
    public static final int ARRAY_LIST = 50;
    public static final int LINKED_LIST = 51;
    public static final int STACK = 52;
    public static final int VECTOR = 53;

    // Set
    public static final int HASH_SET = 60;
    public static final int LINKED_HASH_SET = 61;
    public static final int TREE_SET = 62;

    // Map
    public static final int HASH_MAP = 70;
    public static final int LINKED_HASH_MAP = 71;
    public static final int TREE_MAP = 72;
    public static final int HASHTABLE = 73;
    public static final int PROPERTIES = 74;
    public static final int WEAK_HASH_MAP = 75;

    // Platform
    public static final int VALUE_SET = 101;
    public static final int VALUE_SET_VALUE = 102;
    public static final int RECORD = 103;
    public static final int RECORD_SET = 104;


    private static boolean isClass(Object o, Class clazz) {
        return o.getClass().equals(clazz);
    }

    private static boolean isClass(Class c, Class clazz) {
        return c.equals(clazz);
    }

    public static void write(Encoder out, Object o, boolean flush) throws IOException {
        if (o == null) {
            out.writeEnum(NULL);
        }

        // Writable
        else if (o instanceof Writable) {
            out.writeEnum(WRITABLE);
            out.writeString(o.getClass().getName());
            writeWritable(out, (Writable) o);
        }

        // Primitive
        else if (isClass(o, String.class)) {
            out.writeEnum(STRING);
            out.writeString((String) o);
        } else if (isClass(o, Boolean.class)) {
            out.writeEnum(BOOLEAN);
            out.writeBoolean((Boolean) o);
        } else if (isClass(o, Long.class)) {
            out.writeEnum(LONG);
            out.writeLong((Long) o);
        } else if (isClass(o, Integer.class)) {
            out.writeEnum(INT);
            out.writeInt((Integer) o);
        } else if (isClass(o, Byte.class)) {
            out.writeEnum(BYTE);
            out.writeInt((Byte) o);
        } else if (isClass(o, Short.class)) {
            out.writeEnum(SHORT);
            out.writeInt((Short) o);
        } else if (isClass(o, Float.class)) {
            out.writeEnum(FLOAT);
            out.writeFloat((Float) o);
        } else if (isClass(o, Double.class)) {
            out.writeEnum(DOUBLE);
            out.writeDouble((Double) o);
        } else if (isClass(o, Character.class)) {
            out.writeEnum(CHAR);
            out.writeInt((Character) o);
        }

        // Platform
        else if (isClass(o, Record.class)) {
            out.writeEnum(RECORD);
            writeStringKeyMap(out, (Record) o);
        } else if (isClass(o, RecordSet.class)) {
            out.writeEnum(RECORD_SET);
            writeRecords(out, (RecordSet) o);
        }

        // Ext
        else if (isClass(o, BigInteger.class)) {
            out.writeEnum(BIG_INTEGER);
            out.writeBytes(((BigInteger) o).toByteArray());
        } else if (isClass(o, Date.class)) {
            out.writeEnum(DATE);
            out.writeLong(((Date) o).getTime());
        } else if (o instanceof JsonNode) {
            out.writeEnum(JSON);
            out.writeString(JsonHelper.toJson(o, false));
        } else if (isClass(o, ByteBuffer.class)) {
            out.writeEnum(BYTE_BUFFER);
            out.writeBytes((ByteBuffer) o);
        }

        // Collections
        else if (o instanceof Collection) {
            if (isClass(o, ArrayList.class))
                out.writeEnum(ARRAY_LIST);
            else if (isClass(o, LinkedList.class))
                out.writeEnum(LINKED_LIST);
            else if (isClass(o, Vector.class))
                out.writeEnum(VECTOR);
            else if (isClass(o, Stack.class))
                out.writeEnum(STACK);
            else if (isClass(o, HashSet.class))
                out.writeEnum(HASH_SET);
            else if (isClass(o, LinkedHashSet.class))
                out.writeEnum(LINKED_HASH_SET);
            else if (isClass(o, TreeSet.class))
                out.writeEnum(TREE_SET);
            else {
                if (o instanceof List)
                    out.writeEnum(ARRAY_LIST);
                else if (o instanceof Set)
                    out.writeEnum(LINKED_HASH_SET);
                else
                    throw new IllegalArgumentException();
            }

            writeCollection(out, (Collection) o);
        }

        // Map
        else if (o instanceof Map) {
            if (isClass(o, HashMap.class))
                out.writeEnum(HASH_MAP);
            else if (isClass(o, LinkedHashMap.class))
                out.writeEnum(LINKED_HASH_MAP);
            else if (isClass(o, TreeMap.class))
                out.writeEnum(TREE_MAP);
            else if (isClass(o, Hashtable.class))
                out.writeEnum(HASHTABLE);
            else if (isClass(o, Properties.class))
                out.writeEnum(PROPERTIES);
            else if (isClass(o, WeakHashMap.class))
                out.writeEnum(WEAK_HASH_MAP);
            else
                out.writeEnum(LINKED_HASH_MAP);

            writeCustomKeyMap(out, (Map) o);
        }

        // Array
        else if (o.getClass().isArray()) {
            writeArrayWiteType(out, o);
        }

        // Java Serializable
        else if (o instanceof Serializable) {
            writeSerializable(out, (Serializable) o);
        }

        // Error
        else {
            throw new IllegalArgumentException();
        }

        if (flush)
            out.flush();
    }

    private static void writeArrayWiteType(Encoder out, Object o) throws IOException {
        if (isClass(o, boolean[].class)) {
            boolean[] a = (boolean[]) o;
            out.writeEnum(BOOLEAN_ARRAY);
            out.writeArrayStart();
            out.setItemCount(a.length);
            for (boolean e : a)
                out.writeBoolean(e);
            out.writeArrayEnd();
        } else if (isClass(o, byte[].class)) {
            out.writeEnum(BYTE_ARRAY);
            out.writeBytes((byte[]) o);
        } else if (isClass(o, short[].class)) {
            short[] a = (short[]) o;
            out.writeEnum(SHORT_ARRAY);
            out.writeArrayStart();
            out.setItemCount(a.length);
            for (short e : a)
                out.writeInt(e);
            out.writeArrayEnd();
        } else if (isClass(o, int[].class)) {
            int[] a = (int[]) o;
            out.writeEnum(INT_ARRAY);
            out.writeArrayStart();
            out.setItemCount(a.length);
            for (int e : a)
                out.writeInt(e);
            out.writeArrayEnd();
        } else if (isClass(o, long[].class)) {
            long[] a = (long[]) o;
            out.writeEnum(LONG_ARRAY);
            out.writeArrayStart();
            out.setItemCount(a.length);
            for (long e : a)
                out.writeLong(e);
            out.writeArrayEnd();
        } else if (isClass(o, float[].class)) {
            float[] a = (float[]) o;
            out.writeEnum(FLOAT_ARRAY);
            out.writeArrayStart();
            out.setItemCount(a.length);
            for (float e : a)
                out.writeFloat(e);
            out.writeArrayEnd();
        } else if (isClass(o, double[].class)) {
            double[] a = (double[]) o;
            out.writeEnum(DOUBLE_ARRAY);
            out.writeArrayStart();
            out.setItemCount(a.length);
            for (double e : a)
                out.writeDouble(e);
            out.writeArrayEnd();
        } else if (isClass(o, char[].class)) {
            char[] a = (char[]) o;
            out.writeEnum(CHAR_ARRAY);
            out.writeArrayStart();
            out.setItemCount(a.length);
            for (char e : a)
                out.writeInt(e);
            out.writeArrayEnd();
        }
        // Object array
        else {
            int len = Array.getLength(o);
            out.writeEnum(OBJECT_ARRAY);
            out.writeString(o.getClass().getComponentType().getName());
            out.writeArrayStart();
            out.setItemCount(len);
            for (int i = 0; i < len; i++)
                write(out, Array.get(o, i), false);
            out.writeArrayEnd();
        }
    }


    private static void writeRecords(Encoder out, RecordSet recs) throws IOException {
        out.writeArrayStart();
        out.setItemCount(recs.size());
        for (Record rec : recs) {
            out.startItem();
            writeStringKeyMap(out, rec);
        }
        out.writeArrayEnd();
    }

    private static void writeCollection(Encoder out, Collection c) throws IOException {
        out.writeArrayStart();
        out.setItemCount(c.size());
        for (Object o : c) {
            out.startItem();
            write(out, o, false);
        }
        out.writeArrayEnd();
    }

    private static void writeStringKeyMap(Encoder out, Map m) throws IOException {
        out.writeMapStart();
        out.setItemCount(m.size());
        for (Object e0 : m.entrySet()) {
            Map.Entry e = (Map.Entry) e0;
            out.startItem();
            out.writeString(ObjectUtils.toString(e.getKey()));
            write(out, e.getValue(), false);
        }
        out.writeMapEnd();
    }

    private static void writeCustomKeyMap(Encoder out, Map m) throws IOException {
        out.writeArrayStart();
        out.setItemCount(m.size());
        for (Object e0 : m.entrySet()) {
            Map.Entry e = (Map.Entry) e0;
            out.startItem();
            write(out, e.getKey(), false);
            write(out, e.getValue(), false);
        }
        out.writeArrayEnd();
    }

    private static void writeWritable(Encoder out, Writable o) throws IOException {
        o.write(out, false);
    }

    private static void writeSerializable(Encoder out, Serializable o) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bytes);
        try {
            oos.writeObject(o);
        } finally {
            IOUtils.closeQuietly(oos);
        }
        out.writeBytes(bytes.toByteArray());
    }


    public static Object read(Decoder in) throws IOException {
        int t = in.readEnum();
        switch (t) {
            // Common
            case NULL:
                return null;
            case WRITABLE: {
                Class clazz = ClassHelper.forName(in.readString(null).toString());
                Writable o = (Writable) ClassHelper.newInstance(clazz);
                o.readIn(in);
                return o;
            }

            // Primitive
            case BOOLEAN:
                return in.readBoolean();
            case BYTE:
                return (byte) in.readInt();
            case SHORT:
                return (short) in.readInt();
            case INT:
                return in.readInt();
            case LONG:
                return in.readLong();
            case FLOAT:
                return in.readFloat();
            case DOUBLE:
                return in.readDouble();
            case CHAR:
                return (char) in.readInt();
            case STRING:
                return in.readString(null).toString();

            // Platform
            case RECORD:
                return readStringKeyMap(in, new Record());
            case RECORD_SET:
                return readRecords(in, new RecordSet());

            // EXT
            case BIG_INTEGER:
                return new BigInteger(in.readBytes(null).array());
            case DATE:
                return new Date(in.readLong());
            case JSON:
                return JsonHelper.parse(in.readString(null).toString());
            case BYTE_BUFFER:
                return in.readBytes(null);

            // Collections
            case ARRAY_LIST:
                return readCollection(in, new ArrayList());
            case LINKED_LIST:
                return readCollection(in, new LinkedList());
            case STACK:
                return readCollection(in, new Stack());
            case VECTOR:
                return readCollection(in, new Vector());
            case HASH_SET:
                return readCollection(in, new HashSet());
            case LINKED_HASH_SET:
                return readCollection(in, new LinkedHashSet());
            case TREE_SET:
                return readCollection(in, new TreeSet());

            // Map
            case HASH_MAP:
                return readCustomKeyMap(in, new HashMap());
            case LINKED_HASH_MAP:
                return readCustomKeyMap(in, new LinkedHashMap());
            case TREE_MAP:
                return readCustomKeyMap(in, new TreeMap());
            case HASHTABLE:
                return readCustomKeyMap(in, new Hashtable());
            case PROPERTIES:
                return readCustomKeyMap(in, new Properties());
            case WEAK_HASH_MAP:
                return readCustomKeyMap(in, new WeakHashMap());

            // Array
            case OBJECT_ARRAY:
            case BOOLEAN_ARRAY:
            case BYTE_ARRAY:
            case SHORT_ARRAY:
            case INT_ARRAY:
            case LONG_ARRAY:
            case FLOAT_ARRAY:
            case DOUBLE_ARRAY:
            case CHAR_ARRAY:
                return readArray(in, t);

            // Serializable
            case SERIALIZABLE:
                return readSerializable(in);
            default:
                throw new IOException("Illegal object type " + t);
        }
    }


    @SuppressWarnings("unchecked")
    private static Collection readCollection(Decoder in, Collection c) throws IOException {
        long n = in.readArrayStart();
        if (n > 0) {
            do {
                for (long i = 0; i < n; i++) {
                    Object o = read(in);
                    c.add(o);
                }
            } while ((n = in.arrayNext()) > 0);
        }
        return c;
    }

    @SuppressWarnings("unchecked")
    private static Map readStringKeyMap(Decoder in, Map m) throws IOException {
        long n = in.readMapStart();
        if (n > 0) {
            do {
                for (int i = 0; i < n; i++) {
                    String k = in.readString(null).toString();
                    Object o = read(in);
                    m.put(k, o);
                }
            } while ((n = in.mapNext()) > 0);
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    private static Map readCustomKeyMap(Decoder in, Map m) throws IOException {
        long n = in.readArrayStart();
        if (n > 0) {
            do {
                for (int i = 0; i < n; i++) {
                    Object k = read(in);
                    Object o = read(in);
                    m.put(k, o);
                }
            } while ((n = in.arrayNext()) > 0);
        }
        return m;
    }

    private static RecordSet readRecords(Decoder in, RecordSet recs) throws IOException {
        long n = in.readArrayStart();
        if (n > 0) {
            do {
                for (long i = 0; i < n; i++) {
                    Record rec = new Record();
                    readStringKeyMap(in, rec);
                    recs.add(rec);
                }
            } while ((n = in.arrayNext()) > 0);
        }
        return recs;
    }

    private static Object readSerializable(Decoder in) throws IOException {
        byte[] bytes = in.readBytes(null).array();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        try {
            return ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new ServerException(E.CLASS, e);
        } finally {
            IOUtils.closeQuietly(ois);
        }
    }

    private static Object readArray(Decoder in, int t) throws IOException {
        if (t == BOOLEAN_ARRAY) {
            long n = in.readArrayStart();
            boolean[] a = new boolean[(int) n];
            if (n > 0) {
                do {
                    for (int i = 0; i < n; i++)
                        a[i] = in.readBoolean();
                } while ((n = in.arrayNext()) > 0);
            }
            return a;
        } else if (t == BYTE_ARRAY) {
            return in.readBytes(null).array();
        } else if (t == SHORT_ARRAY) {
            long n = in.readArrayStart();
            short[] a = new short[(int) n];
            if (n > 0) {
                do {
                    for (int i = 0; i < n; i++)
                        a[i] = (short) in.readInt();
                } while ((n = in.arrayNext()) > 0);
            }
            return a;
        } else if (t == INT_ARRAY) {
            long n = in.readArrayStart();
            int[] a = new int[(int) n];
            if (n > 0) {
                do {
                    for (int i = 0; i < n; i++)
                        a[i] = in.readInt();
                } while ((n = in.arrayNext()) > 0);
            }
            return a;
        } else if (t == LONG_ARRAY) {
            long n = in.readArrayStart();
            long[] a = new long[(int) n];
            if (n > 0) {
                do {
                    for (int i = 0; i < n; i++)
                        a[i] = in.readLong();
                } while ((n = in.arrayNext()) > 0);
            }
            return a;
        } else if (t == FLOAT_ARRAY) {
            long n = in.readArrayStart();
            float[] a = new float[(int) n];
            if (n > 0) {
                do {
                    for (int i = 0; i < n; i++)
                        a[i] = in.readFloat();
                } while ((n = in.arrayNext()) > 0);
            }
            return a;
        } else if (t == DOUBLE_ARRAY) {
            long n = in.readArrayStart();
            double[] a = new double[(int) n];
            if (n > 0) {
                do {
                    for (int i = 0; i < n; i++)
                        a[i] = in.readDouble();
                } while ((n = in.arrayNext()) > 0);
            }
            return a;
        } else if (t == CHAR_ARRAY) {
            long n = in.readArrayStart();
            char[] a = new char[(int) n];
            if (n > 0) {
                do {
                    for (int i = 0; i < n; i++)
                        a[i] = (char) in.readInt();
                } while ((n = in.arrayNext()) > 0);
            }
            return a;
        } else if (t == OBJECT_ARRAY) {
            Class et = ClassHelper.forName(in.readString(null).toString());
            long n = in.readArrayStart();
            Object a = Array.newInstance(et, (int) n);
            if (n > 0) {
                do {
                    for (int i = 0; i < n; i++)
                        Array.set(a, i, read(in));
                } while ((n = in.arrayNext()) > 0);
            }
            return a;
        } else {
            throw new IllegalArgumentException();
        }
    }
}
