package com.borqs.server.base.io;


import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;

import java.io.IOException;
import java.io.OutputStream;

public class VfsOutputStream extends OutputStream {
    private FileObject object;
    private FileContent content;
    private OutputStream output;

    public VfsOutputStream(String file) throws IOException {
        this(file, false);
    }

    public FileObject getFileObject() {
        return object;
    }

    public VfsOutputStream(String file, boolean append) throws IOException {
        FileSystemManager fsm = VfsUtils.getFileSystemManager();
        object = fsm.resolveFile(file);
        content = object.getContent();
        if (append) {
            output = content.getOutputStream(append);
        } else {
            output = content.getOutputStream();
        }
    }

    public boolean isOpened() {
        return output != null;
    }

    @Override
    public void write(int b) throws IOException {
        output.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        output.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        output.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        output.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            output.close();
        } catch (IOException ignored) {
        } finally {
            output = null;
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
}
