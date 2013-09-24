package com.broqs.server.impl.staticfile;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.sfs.SFS;
import com.borqs.server.platform.util.SystemHelper;
import com.borqs.server.platform.util.image.ImageMagickHelper;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;

public class StaticFileHelper {
    public static final String TEMP_PHOTO_IMAGE_DIR = SystemHelper.getPathInTempDir("temp_photo_image");
    public static final String X640 = "640x640<";
    public static final String X320 = "320x320<";

    static {
        try {
            FileUtils.forceMkdir(new File(TEMP_PHOTO_IMAGE_DIR));
        } catch (IOException e) {
            throw new ServerException(E.IO, e, "Create directory error " + TEMP_PHOTO_IMAGE_DIR);
        }
    }

    public static void savePhotoImage(SFS sfs, FileItem fi, String imageName, String file) {
        String ext = FilenameUtils.getExtension(imageName);

        StringBuilder fileSName = new StringBuilder(file).append("_S").append(".").append(ext);
        String s1 = TEMP_PHOTO_IMAGE_DIR + "/";
        String fileS = s1 + fileSName;
        String s = SystemHelper.getPathInTempDir(fileS);

        StringBuilder fileMName = new StringBuilder(file).append("_O").append(".").append(ext);
        String fileM = s1 + fileMName;
        //String m = SystemHelper.getPathInTempDir(fileM);

        StringBuilder fileLName = new StringBuilder(file).append("_L").append(".").append(ext);
        String fileL = s1 + fileLName;
        String l = SystemHelper.getPathInTempDir(fileL);

        StringBuilder fileTName = new StringBuilder(file).append("_T").append(".").append(ext);
        String fileT = s1 + fileTName;
        String t = SystemHelper.getPathInTempDir(fileT);

        String tmp = SystemHelper.getPathInTempDir(fileM + "." + ext);

        try {
            //FileUtils.writeByteArrayToFile(new File(tmp), buff);
            fi.write(new File(tmp));
        } catch (IOException e) {
            throw new ServerException(E.RESIZE_LINK_IMAGE, "resize link image error", e);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            ImageMagickHelper.resize4(tmp, s, X320, null, null, l, X640,t,ImageMagickHelper.DEFAULT_THUMBNAIL_SIZE);
        } catch (Exception e) {
            throw new ServerException(E.RESIZE_LINK_IMAGE, "2", e);
        }
        try {
            sfs.writeFile(fileSName.toString(), s);
            sfs.writeFile(fileLName.toString(), l);
            sfs.writeFile(fileMName.toString(), tmp);
        } catch (Exception e) {
            throw new ServerException(E.RESIZE_LINK_IMAGE, "5", e);
        } finally {
            FileUtils.deleteQuietly(new File(tmp));
            FileUtils.deleteQuietly(new File(s));
            FileUtils.deleteQuietly(new File(l));
            //FileUtils.deleteQuietly(new File(m));
        }
    }
}
