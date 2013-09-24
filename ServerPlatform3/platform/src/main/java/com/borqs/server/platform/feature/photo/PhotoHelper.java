package com.borqs.server.platform.feature.photo;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.sfs.SFS;
import com.borqs.server.platform.util.SystemHelper;
import com.borqs.server.platform.util.image.ImageMagickHelper;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;

public class PhotoHelper {
    private static final String TEMP_PHOTO_IMAGE_DIR = SystemHelper.getPathInTempDir("temp_photo_image");
    static {
        try {
            FileUtils.forceMkdir(new File(TEMP_PHOTO_IMAGE_DIR));
        } catch (IOException e) {
            throw new ServerException(E.IO, e, "Create directory error " + TEMP_PHOTO_IMAGE_DIR);
        }
    }

    public static void saveUploadedPhoto(Context ctx, SFS sfs, FileItem imageFileItem, long userId, long albumId, long photoId) throws IOException {
        final String SMALL_SIZE = "360x360>";
        final String LARGE_SIZE = "640x640>";

        String ext = FilenameUtils.getExtension(imageFileItem.getName());
        String orgImageName = String.format("%s_%s_%s_O%s", userId, albumId, photoId, ext);
        String smallImageName = String.format("%s_%s_%s_S%s", userId, albumId, photoId, ext);
        String largeImageName = String.format("%s_%s_%s_L%s", userId, albumId, photoId, ext);
        String thumbnailImageName = String.format("%s_%s_%s_T%s", userId, albumId, photoId, ext);

        String orgImagePath = SystemHelper.getPathInTempDir(TEMP_PHOTO_IMAGE_DIR + "/" + orgImageName);
        String smallImagePath = SystemHelper.getPathInTempDir(TEMP_PHOTO_IMAGE_DIR + "/" + smallImageName);
        String largeImagePath = SystemHelper.getPathInTempDir(TEMP_PHOTO_IMAGE_DIR + "/" + largeImageName);
        String thumbnailImagePath = SystemHelper.getPathInTempDir(TEMP_PHOTO_IMAGE_DIR + "/" + thumbnailImageName);
        try {
            imageFileItem.write(new File(orgImagePath));
        } catch (Exception e) {
            throw new ServerException(E.IO, "Write upload file error " + orgImagePath);
        }

        try {
            ImageMagickHelper.resize4(orgImagePath,
                    smallImagePath, SMALL_SIZE,
                    null, null,
                    largeImagePath, LARGE_SIZE,thumbnailImagePath,ImageMagickHelper.DEFAULT_THUMBNAIL_SIZE);

            sfs.writeFileNoThrow(smallImageName, smallImagePath);
            sfs.writeFileNoThrow(orgImageName, orgImagePath);
            sfs.writeFileNoThrow(largeImageName, largeImagePath);
        } finally {
            FileUtils.deleteQuietly(new File(smallImagePath));
            FileUtils.deleteQuietly(new File(orgImagePath));
            FileUtils.deleteQuietly(new File(largeImagePath));
        }
    }
}
