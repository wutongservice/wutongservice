package com.borqs.server.base.web;


import com.borqs.server.ErrorCode;
import com.borqs.server.base.BaseException;
import com.borqs.server.base.io.Charsets;
import com.borqs.server.base.io.IOException2;
import com.borqs.server.base.util.Copyable;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.StringMap;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class QueryParams extends StringMap implements Copyable<QueryParams> {
    private static final int IN_MEMORY_FILE_SIZE_THRESHOLD = 1024 * 20; // 20KB
    private static final int MAX_UPLOAD_FILE_SIZE = 1024 * 1024 * 50; // 50MB


    private static final String TMP_DIR = getTempDir();

    private static String getTempDir() {
        try {
            String tempDir = FileUtils.getTempDirectoryPath() + "/upload_" + DateUtils.nowNano();
            FileUtils.forceMkdir(new File(tempDir));
            return tempDir;
        } catch (IOException e) {
            throw new IOException2(e);
        }
    }

    private QueryParams() {
    }


    public static QueryParams create(HttpServletRequest req) {
        QueryParams qp = new QueryParams();
        qp.parseParams(req);
        return qp;
    }

    @SuppressWarnings("unchecked")
    private void parseParams(HttpServletRequest req) {
        try {
            Map m = req.getParameterMap();
            for (Object e0 : m.entrySet()) {
                Map.Entry<String, ?> e = (Map.Entry<String, ?>) e0;
                Object v = e.getValue();
                String vs = v instanceof String[]
                        ? StringUtils.join((String[]) v, "")
                        : ObjectUtils.toString(v, "");
                setString(e.getKey(), vs);
            }

            if (ServletFileUpload.isMultipartContent(req)) {
                ServletFileUpload upload = createFileUpload();
                List<FileItem> fileItems = upload.parseRequest(req);
                for (FileItem fileItem : fileItems) {
                    if (fileItem.isFormField()) {
                        setString(fileItem.getFieldName(), fileItem.getString(Charsets.DEFAULT));
                    } else {
                        put(fileItem.getFieldName(), fileItem);
                    }
                }
            }
        } catch (Exception e) {
            throw new WebException(e);
        }
    }

    private static ServletFileUpload createFileUpload() {
        ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory(IN_MEMORY_FILE_SIZE_THRESHOLD, new File(TMP_DIR)));
        upload.setSizeMax(MAX_UPLOAD_FILE_SIZE);
        return upload;
    }

    public FileItem getFile(String k) {
        Object v = get(k);
        return (v != null && v instanceof FileItem) ? (FileItem) v : null;
    }

    public FileItem checkGetFile(String k) {
        FileItem v = getFile(k);
        if (v == null)
            throw new BaseException(ErrorCode.PARAM_ERROR, "Missing file upload parameter '%s'", k);
        return v;
    }

    public Value<String> getSequentialString(String... keys) {
        for (String key : keys) {
            if (containsKey(key)) {
                String v = checkGetString(key);
                return new Value<String>(key, v);
            }
        }
        return null;
    }


    @Override
    public QueryParams copy() {
        QueryParams qp = new QueryParams();
        qp.putAll(this);
        return qp;
    }

    public QueryParams removeKeys(String... keys) {
        for (String key : keys)
            remove(key);
        return this;
    }

    public void close() {
        if (!isEmpty()) {
            for (Object v : values()) {
                if (v instanceof FileItem)
                    ((FileItem) v).delete();
            }
            clear();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public static class Value<T> {
        public final String key;
        public final T value;

        public Value(String key, T value) {
            this.key = key;
            this.value = value;
        }
    }
}
