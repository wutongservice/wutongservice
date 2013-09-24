package com.borqs.server.base.web;


import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import javax.activation.MimetypesFileTypeMap;

public class WebUtils {
    public static String getMimeTypeByFileName(String fileName) {
        String ext = FilenameUtils.getExtension(fileName);
        if (StringUtils.equalsIgnoreCase(ext, "png"))
            return "image/png";

        return new MimetypesFileTypeMap().getContentType(fileName);
    }
}
