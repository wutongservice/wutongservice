package com.borqs.server.base.web;


import com.borqs.server.base.io.Charsets;
import com.borqs.server.base.util.json.JsonUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

public abstract class OutputServlet extends HttpServlet {
    private static final Logger L = LoggerFactory.getLogger(OutputServlet.class);

     /*
     * 取得一个压缩的PrintWriter
     */
     private static PrintWriter getWriter(HttpServletResponse response) throws IOException
     {
        ServletOutputStream sos = response.getOutputStream();
        GZIPOutputStream gzipos = new GZIPOutputStream(sos);
        return new PrintWriter(gzipos);
     }
    
    protected static void output(QueryParams qp, HttpServletRequest req, HttpServletResponse resp, String text, int statusCode, String contentType) throws IOException {
        resp.setCharacterEncoding(Charsets.DEFAULT);
        resp.setStatus(statusCode);
        String callback = qp != null ? qp.getString("callback", "") : req.getParameter("callback");


        if (StringUtils.isBlank(callback)) {
            resp.setContentType(contentType);
        } else {
            resp.setContentType("text/javascript; charset=UTF-8");
        }
        
//        if(text.length() < 100)
//        {
//        	resp.getWriter().print(text);
//        }
//        else
//        {
        	//gzip 压缩
            PrintWriter writer = getWriter(resp);
            resp.setHeader("Content-Encoding","gzip");
            if (StringUtils.isBlank(callback)) {
                writer.write(text);
            } else {
                writer.write(callback);
                if (JsonUtils.isValidate(text)) {
                    writer.write("(");
                    writer.write(text);
                    writer.write(")");
                } else {
                    writer.write("('");
                    writer.write(StringEscapeUtils.escapeJavaScript(text));
                    writer.write("')");
                }
            }
            writer.flush();
            writer.close();
//        }
        if (L.isTraceEnabled())
            L.trace(text);
    }
}
