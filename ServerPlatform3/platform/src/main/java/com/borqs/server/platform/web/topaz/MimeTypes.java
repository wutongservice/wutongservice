package com.borqs.server.platform.web.topaz;


import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import javax.activation.MimetypesFileTypeMap;

public class MimeTypes {
    public static String getMimeTypeByFileName(String fileName) {
        String ext = FilenameUtils.getExtension(fileName);
        if (StringUtils.equalsIgnoreCase(ext, "png"))
            return "image/png";
        if (StringUtils.equalsIgnoreCase(ext, "css"))
            return "plain/text";

        return new MimetypesFileTypeMap().getContentType(fileName);
    }
}
