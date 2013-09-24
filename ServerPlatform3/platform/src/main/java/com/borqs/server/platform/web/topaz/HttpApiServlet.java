package com.borqs.server.platform.web.topaz;

import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.util.SystemHelper;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.platform.web.BatchRun;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.security.Principal;
import java.util.*;

public class HttpApiServlet extends TopazServlet {
    public static final String DOC_DIR = "/doc";

    private boolean responseDocument = false;
    private String documentPrefix = "/@";

    public HttpApiServlet() {
    }

    protected Request createRequest(HttpServletRequest hreq, HttpServletResponse hresp) {
        return new ApiRequest(hreq);
    }

    protected Response createResponse(HttpServletRequest hreq, HttpServletResponse hresp) {
        Response resp = new ApiResponse(hresp);
        resp.setErrorDetail(errorDetail);
        resp.setGzip(outputCompress);
        return resp;
    }

    public boolean isResponseDocument() {
        return responseDocument;
    }

    public void setResponseDocument(boolean responseDocument) {
        this.responseDocument = responseDocument;
    }

    public String getDocumentPrefix() {
        return documentPrefix;
    }

    public void setDocumentPrefix(String documentPrefix) {
        this.documentPrefix = documentPrefix;
    }

    @Override
    protected void process(HttpServletRequest hreq, HttpServletResponse hresp) throws ServletException, IOException {
        if (responseDocument) {
            String path = hreq.getPathInfo();
            if (path.equals(documentPrefix) || path.equals(documentPrefix + "/")) {
                hresp.sendRedirect(documentPrefix + "/index.html");
                return;
            }

            if (path.startsWith(documentPrefix + "/")) {
                String file = StringUtils.removeStart(StringUtils.removeStart(path, documentPrefix), "/");
                writeDocument(hreq, hresp, file);
            } else {
                processApiRequest(hreq, hresp);
            }
        } else {
            processApiRequest(hreq, hresp);
        }
    }

    protected void processApiRequest(HttpServletRequest hreq, HttpServletResponse hresp) throws ServletException, IOException {
        if (hreq.getPathInfo().equals("/batch_run")) {
            Request req = createRequest(hreq, hresp);
            Response resp = createResponse(hreq, hresp);

            try {
                JsonNode body = JsonHelper.parse(IOUtils.toString(hreq.getInputStream(), Charsets.DEFAULT));
                List<BatchRun.EntryRequest> hreqs0 = BatchRun.EntryRequest.fromJson(hreq, body);
                final ArrayList<BatchRun.EntryResponse> hresps0 = new ArrayList<BatchRun.EntryResponse>();
                try {
                    for (BatchRun.EntryRequest hreq0 : hreqs0) {
                        BatchRun.EntryResponse hresp0 = new BatchRun.EntryResponse(hresp);
                        super.process(hreq0, hresp0);
                        hresps0.add(hresp0);
                    }
                } finally {
                    for (BatchRun.EntryResponse hresp0 : hresps0)
                        IOUtils.closeQuietly(hresp0);
                }
                String json = JsonHelper.toJson(new JsonGenerateHandler() {
                    @Override
                    public void generate(JsonGenerator jg, Object arg) throws IOException {
                        jg.writeStartArray();
                        for (BatchRun.EntryResponse hresp0 : hresps0)
                            hresp0.serialize(jg);
                        jg.writeEndArray();
                    }
                }, true);
                resp.body(RawText.of(json)).type(Response.DEFAULT_CONTENT_TYPE);
            } finally {
                resp.doOutput(Response.OutputOptions.fromRequest(req));
            }
        } else {
            super.process(hreq, hresp);
        }
    }

    protected void writeDocument(HttpServletRequest hreq, HttpServletResponse hresp, String file) {
        String home = SystemHelper.getHomeDirectory();
        if (StringUtils.isEmpty(home))
            return;

        file = FilenameUtils.normalizeNoEndSeparator(home) + FilenameUtils.concat(DOC_DIR, file);
        try {
            FileInputStream in = null;
            try {
                String mime = MimeTypes.getMimeTypeByFileName(file);
                hresp.setContentType(mime);
                in = new FileInputStream(file);
                OutputStream out = hresp.getOutputStream();
                IOUtils.copy(in, out);
            } finally {
                IOUtils.closeQuietly(in);
            }
        } catch (IOException e) {
            hresp.setStatus(404);
        }

    }


}
