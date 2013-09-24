package com.borqs.server.market.sfs;


import com.borqs.server.market.utils.FileUtils2;
import com.borqs.server.market.utils.MimeTypeUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;

import javax.servlet.http.HttpServletResponse;
import java.io.*;

public class LocalStorage implements FileStorage {

    private String root;

    public LocalStorage() {
    }

    public LocalStorage(String root) {
        this.root = root;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    private void ensureRoot() throws IOException {
        FileUtils.forceMkdir(new File(root));
    }

    private String getPath(String fileId) {
        return FilenameUtils.concat(root, fileId);
    }

    @Override
    public void init() {
    }

    @Override
    public String write(String fileId, FileContent content) throws IOException {
        Validate.notNull(fileId);
        Validate.notNull(content);

        ensureRoot();

        String path = getPath(fileId);
        OutputStream out = new FileOutputStream(path);
        try {
            IOUtils.copy(content.stream, out);
        } finally {
            IOUtils.closeQuietly(content.stream);
            IOUtils.closeQuietly(out);
        }

        return fileId;
    }

    @Override
    public FileContent read(String fileId) throws IOException {
        String path = getPath(fileId);
        return FileContent.createWithFile(path, MimeTypeUtils.getMimeTypeByFilename(path));
    }
}
