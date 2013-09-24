package com.borqs.server.platform.util;


import com.borqs.server.platform.io.Charsets;

public class BytesSerialization {
    public static byte[] stringToBytes(String s) {
        return Charsets.toBytes(s);
    }

    public static String bytesToString(byte[] bytes) {
        return Charsets.fromBytes(bytes);
    }

    public static byte[] longToBytes(long v) {
        byte[] bytes = new byte[8];
        bytes[0] = (byte) (v >>> 56);
        bytes[1] = (byte) (v >>> 48);
        bytes[2] = (byte) (v >>> 40);
        bytes[3] = (byte) (v >>> 32);
        bytes[4] = (byte) (v >>> 24);
        bytes[5] = (byte) (v >>> 16);
        bytes[6] = (byte) (v >>> 8);
        bytes[7] = (byte) (v);
        return bytes;
    }

    public static long bytesToLong(byte[] bytes) {
        return (((long) bytes[0] << 56) +
                ((long)(bytes[1] & 255) << 48) +
		        ((long)(bytes[2] & 255) << 40) +
                ((long)(bytes[3] & 255) << 32) +
                ((long)(bytes[4] & 255) << 24) +
                ((bytes[5] & 255) << 16) +
                ((bytes[6] & 255) <<  8) +
                ((bytes[7] & 255)));
    }
}
