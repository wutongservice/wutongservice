package com.borqs.server.platform.io;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.commons.io.IOUtils;

import java.io.*;

public class IOHelper {

    public static byte[] toBytes(Object o) {
        try {
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            Encoder out = EncoderFactory.get().binaryEncoder(bytesOut, null);
            RW.write(out, o, true);
            return bytesOut.toByteArray();
        } catch (IOException e) {
            throw new ServerException(E.IO, e);
        }
    }

    public static Object fromBytes(byte[] bytes, int off, int len) {
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes, off, len);
        Decoder in = DecoderFactory.get().binaryDecoder(bytesIn, null);

        try {
            return RW.read(in);
        } catch (IOException e) {
            throw new ServerException(E.IO, e);
        }
    }

    public static Object fromBytes(byte[] bytes) {
        return fromBytes(bytes, 0, bytes.length);
    }

    public static boolean writeTextFile(File file, String text, boolean append) {
        FileWriter w = null;
        try {
            w = new FileWriter(file, append);
            w.write(text);
            w.flush();
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            IOUtils.closeQuietly(w);
        }
    }

    public static String readTextFile(File file, String def) {
        FileReader r = null;
        try {
            r = new FileReader(file);
            return IOUtils.toString(r);
        } catch (IOException e) {
            return def;
        } finally {
            IOUtils.closeQuietly(r);
        }
    }
}
