package com.borqs.server.market.utils;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;

public class FileUtils2 {

    public static String homePath(String name) {
        String home = FileUtils.getUserDirectoryPath();
        return StringUtils.isEmpty(name)
                ? home
                : FilenameUtils.concat(home, name);
    }

    public static void ensureDirectory(String dir) throws IOException {
        FileUtils.forceMkdir(new File(dir));
    }
}
