package com.borqs.server.platform.sfs;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface SFS {
    boolean exists(String name);

    void delete(String name) throws IOException;
    void deleteNoThrow(String name);

    InputStream read(String name) throws IOException;
    InputStream readNoThrow(String name);

    byte[] readBytes(String name) throws IOException;
    byte[] readBytesNoThrow(String name);

    void write(String name, InputStream in) throws IOException;
    boolean writeNoThrow(String name, InputStream in);
    void writeBytes(String name, byte[] bytes, int off, int len) throws IOException;
    boolean writeBytesNoThrow(String name, byte[] bytes, int off, int len);
    void writeBytes(String name, byte[] bytes) throws IOException;
    boolean writeBytesNoThrow(String name, byte[] bytes);

    void writeFile(String name, String localFile) throws IOException;
    boolean writeFileNoThrow(String name, String localFile);
}
