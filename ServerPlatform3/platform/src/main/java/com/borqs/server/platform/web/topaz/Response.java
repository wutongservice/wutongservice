package com.borqs.server.platform.web.topaz;


import com.borqs.server.platform.feature.app.AppSign;
import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.platform.web.BatchRun;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class Response {
    public static final String DEFAULT_CONTENT_TYPE = "text/plain";

    public final HttpServletResponse httpResponse;
    protected boolean gzip;
    protected boolean errorDetail;

    protected int status = 200;
    protected String type = "text/plain";
    protected String charset = Charsets.DEFAULT;
    protected final Map<String, Object> header = new LinkedHashMap<String, Object>();
    protected Object body;

    private String redirect;

    public Response(HttpServletResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    public boolean isErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(boolean errorDetail) {
        this.errorDetail = errorDetail;
    }

    public boolean isGzip() {
        return gzip;
    }

    public void setGzip(boolean gzip) {
        this.gzip = gzip;
    }

    public Response status(int status) {
        this.status = status;
        return this;
    }

    public Response type(String type) {
        this.type = type;
        return this;
    }

    public Response charset(String charset) {
        this.charset = charset;
        return this;
    }

    public Response header(Map<String, Object> header) {
        this.header.putAll(header);
        return this;
    }

    public Response header(String name, Object value) {
        this.header.put(name, value);
        return this;
    }

    public Response header(Object[][] header) {
        return header(CollectionsHelper.arraysToMap(header));
    }

    public Response body(Object body) {
        this.body = body;
        return this;
    }

    public Response error(Throwable t) {
        status(500);
        return body(t);
    }

    public Response redirect(String url) {
        this.redirect = url;
        return this;
    }

    public void doOutput(OutputOptions opts) throws IOException {
        // redirect
        if (StringUtils.isNotBlank(redirect)) {
            httpResponse.sendRedirect(redirect);
            return;
        }

        // status
        httpResponse.setStatus(body instanceof HaltException ? ((HaltException) body).status : status);

        // content type
        httpResponse.setContentType(StringUtils.isNotEmpty(type) ? type : getDefaultContentType());

        // content charset
        if (StringUtils.isNotEmpty(charset))
            httpResponse.setCharacterEncoding(charset);

        // header
        if (MapUtils.isNotEmpty(header)) {
            for (Map.Entry<String, Object> e : header.entrySet())
                httpResponse.setHeader(e.getKey(), ObjectUtils.toString(e.getValue()));
        }
        if (gzip)
            httpResponse.setHeader("Content-Encoding", "gzip");

        // body
        outputBody(opts);
        httpResponse.flushBuffer();
    }

    protected String getDefaultContentType() {
        return DEFAULT_CONTENT_TYPE;
    }

    protected void outputBody(OutputOptions opts) throws IOException {
        if (body instanceof Throwable) {
            outputError((Throwable) body, opts);
        } else if (body instanceof TopazOutput) {
            ((TopazOutput) body).topazOutput(this, opts);
        } else if (body instanceof InputStream) {
            InputStream in = (InputStream) body;
            try {
                IOUtils.copy(in, httpResponse.getOutputStream());
            } finally {
                IOUtils.closeQuietly(in);
            }
        } else if (body instanceof byte[]) {
            httpResponse.getOutputStream().write((byte[]) body);
        } else if (body instanceof ByteBuffer) {
            httpResponse.getOutputStream().write(((ByteBuffer) body).array());
        } else {
            outputGenericObject(body, opts);
        }
    }

    protected void outputGenericObject(Object obj, OutputOptions opts) throws IOException {
        directWrite(ObjectUtils.toString(body), opts);
    }

    protected void outputError(Throwable t, OutputOptions opts) throws IOException {
        if (t instanceof HaltException) {
            HaltException h = (HaltException) t;
            directWrite(h.body, opts);
        } else {
            if (errorDetail) {
                StringWriter buff = new StringWriter();
                PrintWriter w = new PrintWriter(buff);
                w.println(t.getMessage());
                t.printStackTrace(w);
                w.flush();
                directWrite(buff.toString(), opts);
            } else {
                directWrite(t.getMessage(), opts);
            }
        }
    }

    public void directWrite(String s, OutputOptions opts) throws IOException {
        if (opts.jsonpOutput()) {
            StringBuilder buff = new StringBuilder();
            buff.append(opts.jsonCallback).append('(');
            if (JsonHelper.isJson(s)) {
                buff.append(s);
            } else {
                buff.append('\'').append(StringEscapeUtils.escapeJavaScript(s)).append('\'');
            }
            buff.append(')');
            s = buff.toString();
        }

        if (gzip && !(httpResponse instanceof BatchRun.EntryResponse)) {
            PrintWriter w = null;
            try {
                w = new PrintWriter(new GZIPOutputStream(httpResponse.getOutputStream()));
                w.print(s);
            } finally {
                IOUtils.closeQuietly(w);
            }
        } else {
            httpResponse.getWriter().print(s);
        }
    }

    public static class OutputOptions {
        public final String jsonCallback;

        public OutputOptions(String jsonCallback) {
            this.jsonCallback = jsonCallback;
        }

        public boolean jsonpOutput() {
            return StringUtils.isNotEmpty(jsonCallback);
        }

        public static OutputOptions fromRequest(Request req) {
            String jsonCallback = req.getString(AppSign.PARAM_JSONP_CALLBACK, null);
            return new OutputOptions(jsonCallback);
        }
    }
}
