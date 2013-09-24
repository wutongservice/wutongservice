package com.borqs.server.market.deploy;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.File;
import java.io.IOException;

public class TemporaryDirectories {
    public static File getUploadTempDir() {
        String homeDir = SystemUtils.getUserHome().getAbsolutePath();
        File tmpDir = new File(FilenameUtils.concat(homeDir, ".BorqsMarket/tmp"));
        try {
            if (!tmpDir.exists())
                FileUtils.forceMkdir(tmpDir);
        } catch (IOException ignored) {
            System.err.println("Create update temporary directory error " + tmpDir.getAbsolutePath());
        }
        return tmpDir;
    }

    public static String getUploadTempDirPath() {
        return getUploadTempDir().getAbsolutePath();
    }
}
