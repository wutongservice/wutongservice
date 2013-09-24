package com.borqs.server.platform.sfs.vfs;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sfs.AbstractSFS;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class VfsSFS extends AbstractSFS {

    private static final Logger L = Logger.get(VfsSFS.class);

    private String root;

    public VfsSFS() {
        this("");
    }

    public VfsSFS(String root) {
        this.root = root;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = StringUtils.removeEnd(root, "/");
    }

    protected String getAbsolutePath(String name) {
        return StringUtils.isNotEmpty(name) ? root + "/" + name : name;
    }

    protected String checkName(String name) {
        if (StringUtils.isEmpty(name))
            throw new ServerException(E.SFS, "Invalid name '%s'", name);
        return name;
    }

    @Override
    public boolean exists(String name) {
        String path = getAbsolutePath(checkName(name));
        try {
            FileObject fo = VFS.getManager().resolveFile(path);
            return fo.exists();
        } catch (FileSystemException e) {
            throw new ServerException(E.SFS, e, "Write error '%s'", name);
        }
    }

    @Override
    public InputStream read(String name) throws IOException {
        String path = getAbsolutePath(checkName(name));
        FileObject fo = VFS.getManager().resolveFile(path);
        return fo.getContent().getInputStream();
    }

    @Override
    public void write(String name, InputStream in) throws IOException {
        String path = getAbsolutePath(checkName(name));
        L.debug(null, "SFS: write %s => %s", name, path);
        try {
            FileObject fo = null;
            OutputStream out = null;
            try {
                fo = VFS.getManager().resolveFile(path);
                out = fo.getContent().getOutputStream();
                IOUtils.copy(in, out);
            } finally {
                IOUtils.closeQuietly(out);
                try {
                    if (fo != null)
                        fo.close();
                } catch (FileSystemException ignored) {
                }
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    @Override
    public void delete(String name) throws IOException {
        String path = getAbsolutePath(checkName(name));
        L.debug(null, "SFS: delete %s => %s", name, path);
        FileObject fo = null;
        try {
            fo = VFS.getManager().resolveFile(path);
            fo.delete();
        } finally {
            try {
                if (fo != null)
                    fo.close();
            } catch (FileSystemException ignored) {
            }
        }
    }
}
