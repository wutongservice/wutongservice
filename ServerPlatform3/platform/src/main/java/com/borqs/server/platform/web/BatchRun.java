package com.borqs.server.platform.web;


import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.security.Principal;
import java.util.*;

public class BatchRun {

    // request body
    //    [
    //    {
    //        "relative_url":"place/create",
    //            "headers": [
    //        "Timezone: 2011-03-31T12:27:00-0700;;America/Los_Angeles"
    //        ],
    //        "body": {
    //        "layer_id":"10B",
    //                "name":"1250459724",
    //                "radius":500,
    //                "latitude":45.530768,
    //                "longitude":-122.655583,
    //                "date_from":1301572800,
    //                "date_to":1301583600,
    //                "extra": {
    //            "description":"CEO Group Coaching Program at Doubletree Hotel & Executive Meeting Center Portland - Lloyd Center at 3/31 8:00am",
    //                    "url":"http://loqi.me/28W"
    //        }
    //    }
    //    },
    //    {
    //        "relative_url":"place/create",
    //            "body": {
    //        "layer_id":"10B",
    //                "name":"1250459876",
    //                "radius":500,
    //                "latitude":45.511624,
    //                "longitude":-122.683808,
    //                "date_from":1301576400,
    //                "date_to":1301587200,
    //                "extra": {
    //            "description":"PDX Open Source GIS Unconference Day One at Portland State University (PSU) - Smith Memorial Center at 3/31 9:00am",
    //                    "url":"http://loqi.me/28X"
    //        }
    //    }
    //    }
    //    ]

    // response body
    //    [
    //    {
    //        "code":409,
    //            "headers":[
    //        {
    //            "name":"Date",
    //                "value":"Thu, 31 Mar 2011 18:56:37 GMT"
    //        },
    //        {
    //            "name":"Cache-Control",
    //                "value":"no-store"
    //        },
    //        {
    //            "name":"Location",
    //                "value":"https://api.geoloqi.com/1/place/info/3nE"
    //        },
    //        {
    //            "name":"Content-Length",
    //                "value":"18"
    //        },
    //        {
    //            "name":"Content-Type",
    //                "value":"application/json"
    //        }
    //        ],
    //        "body":{
    //        "place_id":"3nE"
    //    },
    //        "time_ms":123.861
    //    },
    //    {
    //        "code":409,
    //            "headers":[
    //        {
    //            "name":"Date",
    //                "value":"Thu, 31 Mar 2011 18:56:37 GMT"
    //        },
    //        {
    //            "name":"Cache-Control",
    //                "value":"no-store"
    //        },
    //        {
    //            "name":"Location",
    //                "value":"https://api.geoloqi.com/1/place/info/3nF"
    //        },
    //        {
    //            "name":"Content-Length",
    //                "value":"18"
    //        },
    //        {
    //            "name":"Content-Type",
    //                "value":"application/json"
    //        }
    //        ],
    //        "body":{
    //        "place_id":"3nF"
    //    },
    //        "time_ms":132.969
    //    }
    //    ]


    public static class EntryRequest implements HttpServletRequest {
        public final HttpServletRequest target;

        private String method = "GET";
        private String pathInfo;
        private String queryString = "";
        private final MMap headers = new MMap();
        private final MMap queryParams = new MMap();
        private byte[] body = new byte[0];

        public EntryRequest(HttpServletRequest target, JsonNode jn) {
            this.target = target;
            parse(jn);
        }

        @SuppressWarnings("unchecked")
        private void parse(JsonNode jn){
            method = jn.has("method") ? jn.path("method").getValueAsText() : "GET";
            String relativeUrl = jn.path("relative_url").getValueAsText();
            pathInfo = StringUtils.substringBefore(relativeUrl, "?");
            queryString = StringUtils.substringAfter(relativeUrl, "?");

//            List<String> targetHeaderNames = EnumerationUtils.toList(target.getHeaderNames());
//            for (String name : targetHeaderNames) {
//                headers.add(name, target.getHeader(name));
//            }

            JsonNode headersJn = jn.get("headers");
            if (headersJn != null) {
                for (int i = 0; i < headers.size(); i++) {
                    String headerLine = headersJn.get(i).getTextValue();
                    String name = StringUtils.substringBefore(headerLine, ":");
                    String value = StringUtils.substringAfter(headerLine, ":").trim();
                    headers.add(name, value);
                }
            }


            MultiMap<String> m = new MultiMap<String>();
            UrlEncoded.decodeTo(queryString, m, Charsets.DEFAULT);
            for (Map.Entry<String, Object> e : m.entrySet()) {
                Object v = e.getValue();
                if (v instanceof String) {
                    queryParams.add(e.getKey(), v);
                } else if (v instanceof List) {
                    for (String s : (List<String>)v) {
                        queryParams.add(e.getKey(), s);
                    }
                }
            }

            try {
                JsonNode bodyJn = jn.get("body");
                if (bodyJn == null) {
                    body = new byte[0];
                } else if (bodyJn.isBinary()) {
                    body = bodyJn.getBinaryValue();
                } else if (bodyJn.isTextual()) {
                    body = bodyJn.getTextValue().getBytes(Charsets.DEFAULT);
                } else {
                    body = JsonHelper.toJson(bodyJn, false).getBytes(Charsets.DEFAULT);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public static List<EntryRequest> fromJson(HttpServletRequest target, JsonNode jn) {
            ArrayList<EntryRequest> l = new ArrayList<EntryRequest>();
            for (int i = 0; i < jn.size(); i++) {
                l.add(new EntryRequest(target, jn.path(i)));
            }
            return l;
        }

        @Override
        public String getAuthType() {
            return target.getAuthType();
        }

        @Override
        public Cookie[] getCookies() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getDateHeader(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getHeader(String s) {
            return target.getHeader(s);
        }

        @Override
        public Enumeration getHeaders(String s) {
            return headers.getValueEnum(s);
        }

        @Override
        public Enumeration getHeaderNames() {
            return headers.getKeyEnum();
        }

        @Override
        public int getIntHeader(String s) {
            return headers.getFirstValueAsInt(s, -1);
        }

        @Override
        public String getMethod() {
            return method;
        }

        @Override
        public String getPathInfo() {
            return pathInfo;
        }

        @Override
        public String getPathTranslated() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getContextPath() {
            return target.getContextPath();
        }

        @Override
        public String getQueryString() {
            return queryString;
        }

        @Override
        public String getRemoteUser() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isUserInRole(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Principal getUserPrincipal() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRequestedSessionId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRequestURI() {
            return target.getContextPath() + target.getServletPath() + pathInfo;
        }

        @Override
        public StringBuffer getRequestURL() {
            return new StringBuffer(StringUtils.removeEnd(target.getRequestURL().toString(), target.getPathInfo()) + pathInfo);
        }

        @Override
        public String getServletPath() {
            return target.getServletPath();
        }

        @Override
        public HttpSession getSession(boolean b) {
            throw new UnsupportedOperationException();
        }

        @Override
        public HttpSession getSession() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isRequestedSessionIdFromCookie() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isRequestedSessionIdFromURL() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isRequestedSessionIdFromUrl() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getAttribute(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Enumeration getAttributeNames() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getCharacterEncoding() {
            return target.getCharacterEncoding();
        }

        @Override
        public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
            target.setCharacterEncoding(s);
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public String getContentType() {
            return getHeader("Content-Type");
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new BodyInputStream(body);
        }

        @Override
        public String getParameter(String s) {
            return queryParams.getFirstValueAsString(s);
        }

        @Override
        public Enumeration getParameterNames() {
            return queryParams.getKeyEnum();
        }

        @Override
        public String[] getParameterValues(String s) {
            List l = queryParams.getList(s);
            if (l == null)
                return null;

            String[] ss = new String[l.size()];
            for (int i = 0; i < ss.length; i++)
                ss[i] = ObjectUtils.toString(queryParams.get(i));
            return ss;
        }

        @Override
        public Map getParameterMap() {
            LinkedHashMap<String, String[]> m = new LinkedHashMap<String, String[]>();
            for (String name : queryParams.keySet()) {
                m.put(name, getParameterValues(name));
            }
            return m;
        }

        @Override
        public String getProtocol() {
            return target.getProtocol();
        }

        @Override
        public String getScheme() {
            return target.getScheme();
        }

        @Override
        public String getServerName() {
            return target.getServerName();
        }

        @Override
        public int getServerPort() {
            return target.getServerPort();
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), Charsets.DEFAULT));
        }

        @Override
        public String getRemoteAddr() {
            return target.getRemoteAddr();
        }

        @Override
        public String getRemoteHost() {
            return target.getRemoteHost();
        }

        @Override
        public void setAttribute(String s, Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeAttribute(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Locale getLocale() {
            return target.getLocale();
        }

        @Override
        public Enumeration getLocales() {
            return target.getLocales();
        }

        @Override
        public boolean isSecure() {
            return target.isSecure();
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRealPath(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getRemotePort() {
            return target.getRemotePort();
        }

        @Override
        public String getLocalName() {
            return target.getLocalName();
        }

        @Override
        public String getLocalAddr() {
            return target.getLocalAddr();
        }

        @Override
        public int getLocalPort() {
            return target.getLocalPort();
        }



        private static class BodyInputStream extends ServletInputStream {
            private ByteArrayInputStream in;
            private BodyInputStream(byte[] buf) {
                in = new ByteArrayInputStream(buf);
            }

            @Override
            public int read() throws IOException {
                return in.read();
            }
        }
    }

    public static class EntryResponse implements HttpServletResponse, Closeable {
        private HttpServletResponse target;
        private int status = SC_OK;
        private String charsetEncoding = Charsets.DEFAULT;
        private String contentType = "text/plain";
        private int contentLength = -1; // TODO: useful?
        private final MMap headers = new MMap();
        private final BodyOutputStream body = new BodyOutputStream();
        private final PrintWriter writer = new PrintWriter(body, true);

        public EntryResponse(HttpServletResponse target) {
            this.target = target;
        }

        @Override
        public void addCookie(Cookie cookie) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsHeader(String s) {
            return headers.containsKey(s);
        }

        @Override
        public String encodeURL(String s) {
            return target.encodeURL(s);
        }

        @Override
        public String encodeRedirectURL(String s) {
            return target.encodeRedirectURL(s);
        }

        @Override
        public String encodeUrl(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String encodeRedirectUrl(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void sendError(int i, String s) throws IOException {
            target.sendError(i, s);
        }

        @Override
        public void sendError(int i) throws IOException {
            target.sendError(i);
        }

        @Override
        public void sendRedirect(String s) throws IOException {
            target.sendRedirect(s);
        }

        @Override
        public void setDateHeader(String s, long l) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addDateHeader(String s, long l) {
            throw new UnsupportedOperationException();
        }


        @Override
        public void setHeader(String s, String s1) {
            headers.put(s, s1);
        }

        @Override
        public void addHeader(String s, String s1) {
            headers.add(s, s1);
        }

        @Override
        public void setIntHeader(String s, int i) {
            setHeader(s, Integer.toString(i));
        }

        @Override
        public void addIntHeader(String s, int i) {
            addHeader(s, Integer.toString(i));
        }

        @Override
        public void setStatus(int i) {
            status = i;
        }

        @Override
        public void setStatus(int i, String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getCharacterEncoding() {
            return charsetEncoding;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return body;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return writer;
        }

        @Override
        public void setCharacterEncoding(String s) {
            charsetEncoding = s;
        }

        @Override
        public void setContentLength(int i) {
            contentLength = i;
        }

        @Override
        public void setContentType(String s) {
            contentType = s;
        }

        @Override
        public void setBufferSize(int i) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getBufferSize() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void flushBuffer() throws IOException {
            body.flush();
        }

        @Override
        public void resetBuffer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isCommitted() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void reset() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setLocale(Locale locale) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Locale getLocale() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() throws IOException {
            writer.close();
            body.close();
        }


        public void serialize(JsonGenerator jg) throws IOException {
            jg.writeStartObject();

            // code
            jg.writeNumberField("code", status);

            // headers
            jg.writeFieldName("headers");
            jg.writeStartArray();
            for (String name : headers.keySet()) {
                List values = headers.getList(name);
                for (Object o : values) {
                    jg.writeStartObject();
                    jg.writeStringField("name", name);
                    jg.writeStringField("value", ObjectUtils.toString(o));
                    jg.writeEndObject();
                }
            }
            jg.writeEndArray();

            // body
            if (StringUtils.startsWith(contentType, "text/") || StringUtils.endsWith(contentType, "/json")) {
                String body = this.body.getString(charsetEncoding);
                if (JsonHelper.isJson(body)) {
                    JsonNode bodyJn = JsonHelper.parse(body);
                    jg.writeFieldName("body");
                    jg.writeTree(bodyJn);
                } else {
                    jg.writeStringField("body", body);
                }
            } else {
                jg.writeBinaryField("body", body.getBytes());
            }

            jg.writeEndObject();
        }

        private static class BodyOutputStream extends ServletOutputStream {
            private final ByteArrayOutputStream out = new ByteArrayOutputStream();
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }
            public byte[] getBytes() {
                return out.toByteArray();
            }

            public String getString(String charset) {
                try {
                    return new String(getBytes(), charset);
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            public void reset() {
                out.reset();
            }
        }

    }

    public static class MMap extends LinkedHashMap<String, Object> {
        public MMap() {
        }

        public MMap(Map<? extends String, ? extends Object> m) {
            super(m);
        }

        @SuppressWarnings("unchecked")
        public void add(String key, Object value) {
            if (containsKey(key)) {
                Object currentValue = get(key);
                if (currentValue instanceof List) {
                    List currentList = (List)currentValue;
                    if (value instanceof List) {
                        currentList.addAll((List)value);
                    } else {
                        currentList.add(value);
                    }
                } else {
                    ArrayList l = new ArrayList();
                    l.add(currentValue);
                    if (value instanceof List) {
                        l.addAll((List)value);
                    } else {
                        l.add(value);
                    }
                    put(key, l);
                }
            } else {
                put(key, value);
            }
        }

        @SuppressWarnings("unchecked")
        public List<Object> getList(String key) {
            Object value = get(key);
            if (value == null)
                return null;
            if (value instanceof List) {
                return (List<Object>)value;
            } else {
                return Arrays.asList(value);
            }
        }

        public Enumeration getKeyEnum() {
            return IteratorUtils.asEnumeration(keySet().iterator());
        }

        public Enumeration getValueEnum(String key) {
            List<Object> values = getList(key);
            return values != null ? IteratorUtils.asEnumeration(values.iterator()) : null;
        }

        public Object getFirstValue(String key) {
            Object value = get(key);
            if (value == null)
                return null;

            if (value instanceof List) {
                List l = (List)value;
                return l.isEmpty() ? null : l.get(0);
            } else {
                return value;
            }
        }

        public String getFirstValueAsString(String key) {
            return ObjectUtils.toString(getFirstValue(key));
        }

        public int getFirstValueAsInt(String key, int def) {
            Object o = getFirstValue(key);
            if (o == null)
                return def;
            return (int)com.borqs.server.platform.data.Values.toInt(o);
        }
    }
}
