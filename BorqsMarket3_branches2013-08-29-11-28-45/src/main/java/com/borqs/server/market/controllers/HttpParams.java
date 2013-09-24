package com.borqs.server.market.controllers;


import com.borqs.server.market.ServiceException;
import com.borqs.server.market.log.Logger;
import com.borqs.server.market.utils.FileUtils2;
import com.borqs.server.market.utils.Params;
import org.apache.commons.collections.EnumerationUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import static com.borqs.server.market.Errors.E_ILLEGAL_PARAM;

public class HttpParams extends Params {

    private static final Logger L = Logger.get(HttpParams.class);

    private static final String PARAMS_ATTR = "$$httpParams$$";

    private static final String templateDirectory = FileUtils2.homePath("tmp/bm_upload_tmp");
    private static ServletFileUpload fileUpload;

    static {
        try {
            FileUtils2.ensureDirectory(templateDirectory);
            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setRepository(new File(templateDirectory));
            fileUpload = new ServletFileUpload(factory);
        } catch (IOException e) {
            L.fatal(null, "Create upload directory error", e);
            System.err.println("Setup file upload error " + templateDirectory);
        }
    }

    public HttpParams() {
    }

    @SuppressWarnings("unchecked")
    public static HttpParams create(HttpServletRequest req)  {
        Object o = req.getAttribute(PARAMS_ATTR);
        if (o != null) {
            return (HttpParams) o;
        }

        try {
            HttpParams params = new HttpParams();
            for (String paramName : (List<String>) EnumerationUtils.toList(req.getParameterNames())) {
                params.put(paramName, req.getParameter(paramName));
            }

            if (ServletFileUpload.isMultipartContent(req)) {
                List<FileItem> fileItems = fileUpload.parseRequest(req);
                if (fileItems != null) {
                    for (FileItem fi : fileItems) {
                        if (fi == null)
                            continue;
                        if (fi.isFormField()) {
                            params.put(fi.getFieldName(), fi.getString("UTF-8"));
                        } else {
                            if (fi.getSize() > 0)
                                params.put(fi.getFieldName(), fi);
                        }
                    }
                }
            }
            processTagsManagerHiddenFields(params);
            req.setAttribute(PARAMS_ATTR, params);
            return params;
        } catch (Exception e) {
            throw new ServiceException(E_ILLEGAL_PARAM, "Parse upload error", e);
        }
    }

    protected static void processTagsManagerHiddenFields(Params params) {
        final String hiddenPrefix = "hidden-";
        for (String name : new HashSet<String>(params.paramNamesSet())) {
            if (name.startsWith(hiddenPrefix)) {
                Object val = params.get(name);
                String name1 = StringUtils.removeStart(name, hiddenPrefix);
                if (StringUtils.isNotBlank(name1)) {
                    params.put(name1, val);
                    params.getParams().remove(name);
                }
            }
        }
    }

    public void close() {
        for (Object val : this.paramsValues()) {
            if (val instanceof FileItem) {
                try {
                    ((FileItem) val).delete();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static class CleanerInterceptor extends HandlerInterceptorAdapter {
        public CleanerInterceptor() {
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
            HttpParams params = (HttpParams) request.getAttribute(PARAMS_ATTR);
            if (params != null) {
                try {
                    params.close();
                } catch (Exception ignored) {
                }
            }
            request.removeAttribute(PARAMS_ATTR);
        }
    }
}
