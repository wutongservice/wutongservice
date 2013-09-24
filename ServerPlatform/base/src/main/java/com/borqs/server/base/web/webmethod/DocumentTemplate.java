package com.borqs.server.base.web.webmethod;


import com.borqs.server.base.io.TextLoader;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.web.OutputServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DocumentTemplate extends OutputServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (StringUtils2.joinIgnoreNull(req.getServletPath(), req.getPathInfo()).equals("/document/template")) {
            String docTemplate = TextLoader.loadClassPath(WebMethodServlet.class, "document_template.xsl");
            output(null, req, resp, docTemplate, 200, "text/xsl");
        }
    }
}
