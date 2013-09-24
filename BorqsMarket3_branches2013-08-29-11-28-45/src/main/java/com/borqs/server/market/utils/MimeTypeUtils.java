package com.borqs.server.market.utils;


import org.apache.commons.io.FilenameUtils;

import java.util.Map;

public class MimeTypeUtils {
    private static final Map<String, String> EXT_TO_MIME = CC.strMap(
            "jpg=>", "image/jpeg",
            "jpeg=>", "image/jpeg",
            "bmp=>", "image/bmp",
            "gif=>", "image/gif",
            "png=>", "image/png",
            "zip=>", "application/zip"
    );

    public static String getMimeTypeByFilename(String filename) {
        String ext = FilenameUtils.getExtension(filename).toLowerCase();
        String mime = EXT_TO_MIME.get(ext);
        return mime != null ? mime : "application/octet-stream";
    }
}
