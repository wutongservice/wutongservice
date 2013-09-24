package com.borqs.server.platform.feature.account;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.sfs.SFS;
import com.borqs.server.platform.util.SystemHelper;
import com.borqs.server.platform.util.image.ImageMagickHelper;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class ProfileImageHelper {
    public static final String TEMP_PROFILE_IMAGE_DIR = SystemHelper.getPathInTempDir("temp_profile_image");

    static {
        try {
            FileUtils.forceMkdir(new File(TEMP_PROFILE_IMAGE_DIR));
        } catch (IOException e) {
            throw new ServerException(E.IO, e, "Create directory error " + TEMP_PROFILE_IMAGE_DIR);
        }
    }


    public static PhotoInfo saveProfileImage(Context ctx, SFS sfs, long userId, long now, String imagePath) {
        final String SMALL_SIZE = "50x50";
        final String MIDDLE_SIZE = "80x80";
        final String LARGE_SIZE = "180x180";

        String imageName = String.format("profile_%s_%s", userId, now);
        String smallImageName = imageName + "_S.jpg";
        String middleImageName = imageName + "_M.jpg";
        String largeImageName = imageName + "_L.jpg";
        String thumbnialImageName = imageName + "_T.jpg";

        String smallImagePath = SystemHelper.getPathInTempDir(TEMP_PROFILE_IMAGE_DIR + "/" + smallImageName);
        String middleImagePath = SystemHelper.getPathInTempDir(TEMP_PROFILE_IMAGE_DIR + "/" + middleImageName);
        String largeImagePath = SystemHelper.getPathInTempDir(TEMP_PROFILE_IMAGE_DIR + "/" + largeImageName);
        String thumbnailImagePath = SystemHelper.getPathInTempDir(TEMP_PROFILE_IMAGE_DIR + "/" + thumbnialImageName);

        try {
            ImageMagickHelper.resize4(imagePath, smallImagePath, SMALL_SIZE, middleImagePath, MIDDLE_SIZE, largeImagePath, LARGE_SIZE,thumbnailImagePath,ImageMagickHelper.DEFAULT_THUMBNAIL_SIZE);

            sfs.writeFileNoThrow(smallImageName, smallImagePath);
            sfs.writeFileNoThrow(middleImageName, middleImagePath);
            sfs.writeFileNoThrow(largeImageName, largeImagePath);
        } finally {
            FileUtils.deleteQuietly(new File(smallImagePath));
            FileUtils.deleteQuietly(new File(middleImagePath));
            FileUtils.deleteQuietly(new File(largeImagePath));
        }

        return new PhotoInfo(smallImageName, middleImageName, largeImageName);
    }

    public static PhotoInfo saveUploadedProfileImage(Context ctx, SFS sfs, long userId, long now, FileItem imageFileItem) {
        String orgPath = SystemHelper.getPathInTempDir(TEMP_PROFILE_IMAGE_DIR + "/profile_" + userId + "_org.jpg");
        try {
            imageFileItem.write(new File(orgPath));
        } catch (Exception e) {
            throw new ServerException(E.IO, "Write upload file error " + orgPath);
        }
        try {
            return saveProfileImage(ctx, sfs, userId, now, orgPath);
        } finally {
            FileUtils.deleteQuietly(new File(orgPath));
        }
    }
}
