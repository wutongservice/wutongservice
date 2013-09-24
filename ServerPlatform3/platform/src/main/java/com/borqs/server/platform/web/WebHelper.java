package com.borqs.server.platform.web;


import com.borqs.server.platform.util.StringHelper;
import com.borqs.server.platform.util.VfsHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import javax.activation.MimetypesFileTypeMap;
import java.util.*;

public class WebHelper {

    private static MultiValueMap EXT_TO_MIME_TYPE = new MultiValueMap();
    private static MultiValueMap MIME_TYPE_TO_EXT = new MultiValueMap();

    static {
        loadMimeTypes();
//        for (Object k : EXT_TO_MIME_TYPE.keySet()) {
//            Collection mimeTypes = EXT_TO_MIME_TYPE.getCollection(k);
//            System.out.println(k + " => " + StringUtils.join(mimeTypes, ","));
//        }
    }

    @SuppressWarnings("unchecked")
    private static void loadMimeTypes() {
        String all = VfsHelper.loadTextInClasspath(WebHelper.class, "mimetypes.txt");
        String[] lines = StringHelper.splitArray(all, "\n", true);
        for (String line : lines) {
            String ext = StringUtils.substringBefore(line, "=").trim();
            String mimeType = StringUtils.substringAfter(line, "=").trim();
            EXT_TO_MIME_TYPE.put(ext, mimeType);
            MIME_TYPE_TO_EXT.put(mimeType, ext);
        }

        for (Object k : EXT_TO_MIME_TYPE.keySet()) {
            String key = (String) k;
            List<String> mimeTypes = (List) EXT_TO_MIME_TYPE.getCollection(key);
            if (mimeTypes.size() <= 1)
                continue;

            Collections.sort(mimeTypes, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    boolean b1 = o1.contains("x-");
                    boolean b2 = o2.contains("x-");
                    if (b1 && b2)
                        return o1.compareTo(o2);
                    if (b1)
                        return 1;
                    if (b2)
                        return -1;

                    b1 = o1.contains("ms") || o1.contains("mac");
                    b2 = o2.contains("ms") || o2.contains("mac");
                    if (b1 && b2)
                        return o1.compareTo(o2);
                    if (b1)
                        return 1;
                    if (b2)
                        return -1;

                    return o1.compareTo(o2);
                }
            });
        }
    }


    @SuppressWarnings("unchecked")
    public static String getMimeTypeByFileExt(String ext) {
        if (ext.startsWith("."))
            ext = StringUtils.removeStart(ext, ".");
        ext = ext.toLowerCase();
        Collection<String> mimeTypes = EXT_TO_MIME_TYPE.getCollection(ext);
        if (CollectionUtils.isEmpty(mimeTypes))
            return "application/octet-stream";

        return mimeTypes.iterator().next();
    }

    public static String getMimeTypeByFileName(String fileName) {
        String ext = FilenameUtils.getExtension(fileName).toLowerCase();
        return getMimeTypeByFileExt(ext);
    }

    public static String getFileExtByMimeType(String mimeType) {
        String ext = (String) MIME_TYPE_TO_EXT.get(mimeType);
        return ext != null ? ext : "bin";
    }
}
