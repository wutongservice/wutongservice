package com.borqs.server.platform.sfs;


import com.borqs.server.platform.log.Logger;
import org.apache.commons.io.IOUtils;

import java.io.*;

public abstract class AbstractSFS implements SFS {

    private static final Logger L = Logger.get(AbstractSFS.class);


    protected AbstractSFS() {
    }

    @Override
    public void deleteNoThrow(String name) {
        try {
            delete(name);
        } catch (Throwable t) {
            L.error(null, t, "SFS: delete error " + name);
        }
    }

    @Override
    public InputStream readNoThrow(String name) {
        try {
            return read(name);
        } catch (Throwable t) {
            L.error(null, t, "SFS: read error " + name);
            return null;
        }
    }

    @Override
    public byte[] readBytes(String name) throws IOException {
        InputStream in = null;
        try {
            in = read(name);
            return IOUtils.toByteArray(in);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    @Override
    public byte[] readBytesNoThrow(String name) {
        try {
            return readBytes(name);
        } catch (Throwable t) {
            L.error(null, t, "SFS: read bytes error " + name);
            return null;
        }
    }

    @Override
    public boolean writeNoThrow(String name, InputStream in) {
        try {
            write(name, in);
            return true;
        } catch (Throwable t) {
            L.error(null, t, "SFS: write error " + name);
            return false;
        }
    }

    @Override
    public void writeBytes(String name, byte[] bytes, int off, int len) throws IOException {
        ByteArrayInputStream in = null;
        try {
            in = new ByteArrayInputStream(bytes, off, len);
            write(name, in);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    @Override
    public boolean writeBytesNoThrow(String name, byte[] bytes, int off, int len) {
        try {
            writeBytes(name, bytes, off, len);
            return true;
        } catch (Throwable t) {
            L.error(null, t, "SFS: write bytes error " + name);
            return false;
        }
    }

    @Override
    public void writeBytes(String name, byte[] bytes) throws IOException {
        writeBytes(name, bytes, 0, bytes.length);
    }

    @Override
    public boolean writeBytesNoThrow(String name, byte[] bytes) {
        return writeBytesNoThrow(name, bytes, 0, bytes.length);
    }

    @Override
    public void writeFile(String name, String localFile) throws IOException {
        InputStream in = null;
        try {
            in = new FileInputStream(localFile);
            write(name, in);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    @Override
    public boolean writeFileNoThrow(String name, String localFile) {
        try {
            writeFile(name, localFile);
            return true;
        } catch (Throwable t) {
            L.error(null, t, "SFS: write file error " + name);
            return false;
        }
    }
}
