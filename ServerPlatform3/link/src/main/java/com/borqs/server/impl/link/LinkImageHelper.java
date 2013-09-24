package com.borqs.server.impl.link;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.sfs.SFS;
import com.borqs.server.platform.util.RandomHelper;
import com.borqs.server.platform.util.SystemHelper;
import com.borqs.server.platform.util.image.ImageMagickCommand;
import com.borqs.server.platform.util.image.ImageMagickHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;

import java.io.File;
import java.io.IOException;

public class LinkImageHelper {

    public static final String TEMP_LINK_IMAGE_DIR = SystemHelper.getPathInTempDir("temp_link_image");

    static {
        try {
            FileUtils.forceMkdir(new File(TEMP_LINK_IMAGE_DIR));
        } catch (IOException e) {
            throw new ServerException(E.IO, e, "Create directory error " + TEMP_LINK_IMAGE_DIR);
        }
    }

    public static void saveLinkImage(SFS sfs, byte[] buff, String imageName, int w, int h) {
        String ext = FilenameUtils.getExtension(imageName);
        String f = SystemHelper.getPathInTempDir(TEMP_LINK_IMAGE_DIR + "/" + imageName);
        String tmp = SystemHelper.getPathInTempDir(TEMP_LINK_IMAGE_DIR + "/" + RandomHelper.generateId() + "." + ext);

        try {
            FileUtils.writeByteArrayToFile(new File(tmp), buff);
            new ImageMagickCommand(tmp, f).resize(w, h).run();
            sfs.writeFile(imageName, f);
        } catch (IOException e) {
            throw new ServerException(E.RESIZE_LINK_IMAGE, "resize link image error", e);
        } finally {
            FileUtils.deleteQuietly(new File(tmp));
            FileUtils.deleteQuietly(new File(f));
        }
    }
}
