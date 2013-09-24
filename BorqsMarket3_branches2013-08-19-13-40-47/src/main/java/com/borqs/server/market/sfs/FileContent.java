package com.borqs.server.market.sfs;

import com.borqs.server.market.utils.FileUtils2;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.Validate;

import java.io.*;


public class FileContent {
    public final InputStream stream;
    public final String contentType;
    public final long size;
    public String filename;

    private FileContent(InputStream stream, String contentType, long size) {
        Validate.notNull(stream);
        this.stream = stream;
        this.contentType = contentType;
        this.size = size;
    }


    public FileContent withFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public static FileContent create(InputStream stream, String contentType, long size) {
        return new FileContent(stream, contentType, size);
    }

    public static FileContent createWithFile(String path, String contentType) throws IOException {
        return create(new FileInputStream(path), contentType, FileUtils2.getFileSize(path));
    }

    public static FileContent createWithBytes(byte[] content, String contentType) {
        return create(new ByteArrayInputStream(content), contentType, content.length);
    }

    public static FileContent createWithText(String content, String contentType) throws UnsupportedEncodingException {
        return createWithBytes(content.getBytes("UTF-8"), contentType);
    }

    public static FileContent createWithFileItem(FileItem fileItem) throws IOException {
        Validate.notNull(fileItem);
        Validate.isTrue(!fileItem.isFormField());
        return create(fileItem.getInputStream(), fileItem.getContentType(), fileItem.getSize());
    }
}
