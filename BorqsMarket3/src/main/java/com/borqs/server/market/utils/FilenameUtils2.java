package com.borqs.server.market.utils;


import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

public class FilenameUtils2 {
    public static String changeFilenameWithoutExt(String filename, String newName) {
        String ext = FilenameUtils.getExtension(filename);
        if (StringUtils.isEmpty(ext)) {
            return newName;
        } else {
            return newName + "." + ext;
        }
    }
}
