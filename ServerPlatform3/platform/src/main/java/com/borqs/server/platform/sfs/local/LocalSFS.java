package com.borqs.server.platform.sfs.local;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sfs.AbstractSFS;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;

public class LocalSFS extends AbstractSFS {
    private static final Logger L = Logger.get(LocalSFS.class);

    private String root;

    public LocalSFS() {
        this("");
    }

    public LocalSFS(String root) {
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
        File f = new File(path);
        return f.isFile() && f.exists();
    }

    @Override
    public InputStream read(String name) throws IOException {
        String path = getAbsolutePath(checkName(name));
        L.debug(null, "SFS: read %s <= %s", name, path);
        return new FileInputStream(path);
    }

    @Override
    public void write(String name, InputStream in) throws IOException {
        String path = getAbsolutePath(checkName(name));
        L.debug(null, "SFS: write %s => %s", name, path);
        try {
            OutputStream out = null;
            try {
                out = new FileOutputStream(path);
                IOUtils.copy(in, out);
            } finally {
                IOUtils.closeQuietly(out);
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    @Override
    public void delete(String name) throws IOException {
        String path = getAbsolutePath(checkName(name));
        L.debug(null, "SFS: delete %s => %s", name, path);
        new File(path).delete();
    }
}
