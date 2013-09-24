package com.borqs.server.platform.sfs;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.web.WebHelper;
import com.borqs.server.platform.web.topaz.Response;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.OutputStream;

public class SFSHelper {
    public static void saveUpload(SFS sfs, FileItem fileItem, String name) throws IOException {
        OutputStream out = null;
        try {
            sfs.write(name, fileItem.getInputStream());
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public static void sfsResponse(SFS sfs, Response resp, String name, String contentType) throws IOException {
        if (StringUtils.isBlank(contentType))
            contentType = WebHelper.getMimeTypeByFileName(name);

        resp.type(contentType);
        resp.header("Content-Disposition", "attachment; filename=\"" + name + "\"");
        resp.body(sfs.read(name));

    }

    public static void sfsResponse(SFS sfs, Response resp, String name) throws IOException {
        sfsResponse(sfs, resp, name, null);
    }
}
