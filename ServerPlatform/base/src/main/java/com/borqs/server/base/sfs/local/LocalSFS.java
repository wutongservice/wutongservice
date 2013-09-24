package com.borqs.server.base.sfs.local;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.util.FileUtils2;
import com.borqs.server.base.sfs.AbstractSFS;
import com.borqs.server.base.sfs.SFSException;
import com.borqs.server.base.util.StringUtils2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.Validate;

import javax.servlet.http.HttpServletResponse;
import java.io.*;

public class LocalSFS extends AbstractSFS {
    private String directory;

    public LocalSFS(Configuration conf) {
        this(conf.checkGetString("dir"));
    }

    public LocalSFS(String dir) {
        Validate.notNull(dir);
        dir = FileUtils2.expandPath(dir.trim());
        if (!dir.endsWith("/"))
            dir += "/";

        try {
            FileUtils.forceMkdir(new File(dir));
        } catch (IOException e) {
            throw new SFSException(e);
        }
        this.directory = dir;
    }

    @Override
    public void init() {
    }

    @Override
    public void destroy() {
    }

    private String joinPath(String path) {
        return StringUtils2.joinIgnoreNull(directory, calculateRelativePath(path));
    }

    protected String calculateRelativePath(String file) {
        return file;
    }

    @Override
    public OutputStream create(String file) {
        Validate.notNull(file);
        checkFile(file);

        String path = joinPath(file);
        File f = new File(path);
        if (f.exists())
            throw new SFSException("File is existing (%s)", file);

        try {
            String dir = FilenameUtils.getFullPathNoEndSeparator(path);
            FileUtils.forceMkdir(new File(dir));
            return new FileOutputStream(f, false);
        } catch (IOException e) {
            throw new SFSException(e);
        }
    }

    @Override
    public InputStream read(String file) {
        Validate.notNull(file);

        String path = joinPath(file);
        File f = new File(path);
        if (!f.exists() || !f.isFile())
            throw new SFSException("Open file error (%s)", file);

        try {
            return new FileInputStream(f);
        } catch (IOException e) {
            throw new SFSException(e);
        }
    }

    @Override
    public InputStream read(String file, HttpServletResponse resp) {
        return read(file);
    }

    @Override
    public boolean delete(String file) {
        Validate.notNull(file);
        return new File(joinPath(file)).delete();
    }

    @Override
    public boolean exists(String file) {
        Validate.notNull(file);

        String path = joinPath(file);
        File f = new File(path);
        return f.exists() && f.isFile();
    }
}
