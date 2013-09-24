package com.borqs.server.base.io;


import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;

import java.io.IOException;
import java.io.InputStream;

public class VfsInputStream extends InputStream {
    private FileObject object;
    private FileContent content;
    private InputStream input;

    public VfsInputStream(String file) throws IOException {
        FileSystemManager fsm = VfsUtils.getFileSystemManager();
        object = fsm.resolveFile(file);
        content = object.getContent();
        input = content.getInputStream();
    }

    public boolean isOpened() {
        return input != null;
    }

    public FileObject getFileObject() {
        return object;
    }

    @Override
    public int read() throws IOException {
        return input.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return input.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return input.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return input.skip(n);
    }

    @Override
    public int available() throws IOException {
        return input.available();
    }

    @Override
    public void close() throws IOException {
        try {
            input.close();
        } catch (IOException ignored) {
        } finally {
            input = null;
        }

        try {
            VfsUtils.close(content);
        } catch (IOException ignored) {
        } finally {
            content = null;
        }

        try {
            VfsUtils.close(object);
        } catch (IOException ignored) {
        } finally {
            object = null;
        }
        super.close();
    }

    @Override
    public void mark(int readlimit) {
        input.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        input.reset();
    }

    @Override
    public boolean markSupported() {
        return input.markSupported();
    }
}
