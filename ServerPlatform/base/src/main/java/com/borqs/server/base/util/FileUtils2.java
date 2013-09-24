package com.borqs.server.base.util;


import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

public class FileUtils2 {
    public static String expandPath(String path) {
        if (path.startsWith("~")) {
            return FileUtils.getUserDirectoryPath() + StringUtils.removeStart(path, "~");
        } else {
            return path;
        }
    }

}
