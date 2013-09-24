package com.borqs.server.market.models;


import com.borqs.server.market.sfs.FileContent;
import com.borqs.server.market.sfs.FileStorage;
import com.borqs.server.market.utils.FilenameUtils2;
import com.borqs.server.market.utils.MimeTypeUtils;
import com.borqs.server.market.utils.RandomUtils2;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileStorageUtils {

    public static String saveImageWithFileItem(FileStorage storage, String imageField, String id, FileItem fi) throws IOException {
        if (fi == null)
            return null;

        String imageType = StringUtils.removeEnd(imageField, "_image");
        FileContent content = makeFileContentWithFileItem(fi);
        if (content == null)
            return null;

        String fid = FilenameUtils2.changeFilenameWithoutExt(content.filename, makeProductImageFilename(id, imageType));
        return storage.write(fid, content);
    }

    public static String saveImageWithContent(FileStorage storage, String imageField, String id, InputStream contentStream, String filename) throws IOException {
        if (contentStream == null)
            return null;

        String imageType = StringUtils.removeEnd(imageField, "_image");

        FileContent content = makeFileContentWithContent(contentStream, filename);
        if (content == null)
            return null;

        String fid = FilenameUtils2.changeFilenameWithoutExt(content.filename, makeProductImageFilename(id, imageType));
        return storage.write(fid, content);
    }

    public static String saveImageWithFileItemOrContent(FileStorage storage, String imageField, String id, FileItem fi, InputStream contentStream, String filename) throws IOException {
        if (fi != null && !fi.isFormField()) {
            return saveImageWithFileItem(storage, imageField, id, fi);
        } else if (contentStream != null) {
            return saveImageWithContent(storage, imageField, id, contentStream, filename);
        } else {
            return null;
        }
    }

    public static String saveAvatarImageWithFileItem(FileStorage storage, String accountId, FileItem fi) throws IOException {
        return saveImageWithFileItem(storage, "avatar-image", accountId, fi);
    }

    public static String saveProduct(FileStorage storage, File productFile, String id, int version) throws IOException {
        FileContent content = makeFileContentWithFile(productFile);
        if (content == null)
            return null;

        String fid = FilenameUtils2.changeFilenameWithoutExt(content.filename, makeProductFilename(id, version));
        return storage.write(fid, content);
    }

    private static FileContent makeFileContentWithFile(File file) throws IOException {
        return makeFileContentWithContent(new FileInputStream(file), FilenameUtils.getName(file.getName()));
    }

    private static FileContent makeFileContentWithFileItem(FileItem fi) throws IOException {
        return FileContent.createWithFileItem(fi).withFilename(fi.getName());
    }

    private static FileContent makeFileContentWithContent(InputStream content, String filename) throws IOException {
        String contentType = MimeTypeUtils.getMimeTypeByFilename(filename);
        return FileContent.create(content, contentType, content.available()).withFilename(filename);
    }

    private static String makeProductFilename(String id, int version) {
        return String.format("%s-%s-%06d", id, version, RandomUtils2.randomInt(0, 1000000));
    }

    private static String makeProductImageFilename(String id, String type) {
        return String.format("%s-%s-%06d", id, type, RandomUtils2.randomInt(0, 1000000));
    }

}
