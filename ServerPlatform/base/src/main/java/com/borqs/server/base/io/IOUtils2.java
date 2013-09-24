package com.borqs.server.base.io;


import com.borqs.server.base.data.Null;
import com.borqs.server.base.data.Privacy;
import com.borqs.server.base.data.Values;
import com.borqs.server.base.util.json.JsonUtils;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonNode;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class IOUtils2 {

    private static final int NULL = 0;
    private static final int BOOLEAN = 1;
    private static final int INT = 2;
    private static final int FLOAT = 3;
    private static final int STRING = 4;
    private static final int JSON = 5;
    private static final int PRIVACY = 7;

    public static void writeVariant(Encoder out, Object o, boolean flush) throws IOException {
        if (o == null || o instanceof Null) {
            out.writeEnum(NULL);
        } else if (o instanceof Boolean) {
            out.writeEnum(BOOLEAN);
            out.writeBoolean((Boolean) o);
        } else if (o instanceof Integer || o instanceof Long || o instanceof Short || o instanceof Byte) {
            out.writeEnum(INT);
            out.writeLong(((Number) o).longValue());
        } else if (o instanceof Float || o instanceof Double) {
            out.writeEnum(FLOAT);
            out.writeDouble(((Number) o).doubleValue());
        } else if (o instanceof CharSequence) {
            out.writeEnum(STRING);
            out.writeString((CharSequence)o);
        } else if (o instanceof Character) {
            out.writeEnum(STRING);
            out.writeString(o.toString());
        } else if (o instanceof JsonNode || o instanceof List || o instanceof Map) {
            out.writeEnum(JSON);
            out.writeString(JsonUtils.toJson(o, false));
        } else if (o instanceof Privacy) {
            out.writeEnum(PRIVACY);
        } else {
            Validate.isTrue(false);
        }

        if (flush)
            out.flush();
    }

    public static Object readVariant(Decoder in) throws IOException {
        int e = in.readEnum();
        switch (e) {
            case NULL:
                return Values.NULL;
            case BOOLEAN:
                return in.readBoolean();
            case INT:
                return in.readLong();
            case FLOAT:
                return in.readDouble();
            case STRING:
                return in.readString(null).toString();
            case JSON:
                return JsonUtils.parse(in.readString(null).toString());
            case PRIVACY:
                return Values.PRIVACY;
            default:
                throw new IOException("Read value error");
        }
    }

    public static byte[] toBytes(Serializable o) {
        Validate.notNull(o);

        try {
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            Encoder out = EncoderFactory.get().binaryEncoder(bytesOut, null);
            o.write(out, true);
            return bytesOut.toByteArray();
        } catch (IOException e) {
            throw new IOException2(e);
        }
    }

    public static <T extends Serializable> T fromBytes(Class<T> type, byte[] bytes, int off, int len) {
        Validate.notNull(type);
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes, off, len);
        Decoder in = DecoderFactory.get().binaryDecoder(bytesIn, null);

        try {
            T v = type.newInstance();
            v.readIn(in);
            return v;
        } catch (Exception e) {
            throw new IOException2(e);
        }
    }

    public static <T extends Serializable> T fromBytes(Class<T> type, byte[] bytes) {
        return fromBytes(type, bytes, 0, bytes.length);
    }

    public static ByteBuffer toByteBuffer(Serializable o) {
        return ByteBuffer.wrap(toBytes(o));
    }


    public static <T extends Serializable> T fromByteBuffer(Class<T> type, ByteBuffer byteBuff, int off, int len) {
        if (byteBuff.isDirect() || byteBuff.isReadOnly()) {
            byte[] bytes = new byte[len];
            byteBuff.get(bytes, off, len);
            return fromBytes(type, bytes);
        } else {
            return fromBytes(type, byteBuff.array(), off, len);
        }
    }

    public static <T extends Serializable> T fromByteBuffer(Class<T> type, ByteBuffer byteBuff) {
        return fromByteBuffer(type, byteBuff, 0, byteBuff.capacity());
    }

    public static byte[] loadFileToBytes(File file) {
        if (file.isFile() && file.exists() && file.canRead()) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(file);
                return IOUtils.toByteArray(in);
            } catch (IOException e) {
                return null;
            } finally {
                IOUtils.closeQuietly(in);
            }
        } else {
            return null;
        }
    }

    public static byte[] readInput(InputStream input) {
        Validate.notNull(input);
        try {
            return IOUtils.toByteArray(input);
        } catch (IOException e) {
            throw new IOException2(e);
        }
    }

    public static void writeToFile(File file, InputStream input) {
        Validate.notNull(file);
        Validate.notNull(input);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            IOUtils.copy(input, out);
        } catch (IOException e) {
            throw new IOException2(e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }
}
