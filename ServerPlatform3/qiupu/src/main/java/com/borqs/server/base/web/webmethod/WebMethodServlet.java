package com.borqs.server.base.web.webmethod;


import com.borqs.server.ErrorCode;
import com.borqs.server.ServerException;
import com.borqs.server.base.ResponseError;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Null;
import com.borqs.server.base.data.Privacy;
import com.borqs.server.base.io.Charsets;
import com.borqs.server.base.util.Errors;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.util.json.JsonGenerateHandler;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.base.web.OutputServlet;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.WebException;
import org.apache.avro.AvroRuntimeException;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


public class WebMethodServlet extends OutputServlet {
    private static final Logger L = LoggerFactory.getLogger(WebMethodServlet.class);

    private final Map<String, Invoker> invokers = new HashMap<String, Invoker>();
    protected volatile Configuration configuration;
    protected boolean printErrorDetails = false;

    public WebMethodServlet() {
        initMethods();
    }

    private void initMethods() {
        Class clazz = getClass();
        for (Method method : clazz.getMethods()) {
            if (!Modifier.isPublic(method.getModifiers()))
                continue;

            WebMethod ann = method.getAnnotation(WebMethod.class);
            if (ann == null)
                continue;

            String url = ann.value();
            if (invokers.containsKey(url))
                throw new WebException("Repetitive url '%s'", url);

            for (Class pt : method.getParameterTypes()) {
                if (!pt.isAssignableFrom(HttpServletRequest.class) &&
                        !pt.equals(HttpServletResponse.class) &&
                        !pt.equals(QueryParams.class))
                    throw new WebException("Invalid parameter type '%s'", pt.getName());
            }

            invokers.put(url, new Invoker(this, method));
        }
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void init() throws ServletException {
        super.init();
    }

    protected void initPrintErrorDetails() {
        Configuration conf = getConfiguration();
        printErrorDetails = conf.getBoolean("webMethod.printErrorDetails", true);  // TODO: default should false
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    protected String getXmlDocument() {
        return null;
    }

    protected String getXmlDocumentPath() {
        return null;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doProcess(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doProcess(req, resp);
    }

    protected boolean before(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        L.trace("begin before");
    	resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setCharacterEncoding(Charsets.DEFAULT);
        String ua = req.getHeader("User-Agent");
        L.trace("----" + StringUtils.trimToEmpty(ua));
        if (StringUtils.isNotBlank(ua) && StringUtils.containsIgnoreCase(ua, "Sogou web spider"))
        {
//            L.trace("end before");
        	return false;
        }
//        L.trace("end before");

        return true;
    }

    protected void after(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    }

    protected void process(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        L.trace("begin process");
    	String url = StringUtils.removeStart(StringUtils2.joinIgnoreNull(req.getServletPath(), req.getPathInfo()), "/");
        if (processDocument(url, req, resp))
            return;

        Invoker invoker = invokers.get(url);
        if (invoker != null) {
            invoker.invoke(req, resp);
        } else {
            resp.setStatus(404); // Not Found
        }
//        L.trace("end process");
    }

    private boolean processDocument(String url, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String doc = getXmlDocument();
        if (doc != null && StringUtils.equals(getXmlDocumentPath(), url)) {
            output(null, req, resp, doc, 200, "text/xml");
            return true;
        }
        return false;
    }

    private void doProcess(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (L.isTraceEnabled())
            L.trace("{} {}", req.getMethod(), req.getRequestURI());


        try {
            boolean b = before(req, resp);
            if (b) {
                process(req, resp);
            }
        } finally {
            after(req, resp);
        }


        if (L.isDebugEnabled())
            L.trace("----------------------------------------");
    }


    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String reqMethod = req.getMethod();
        if (reqMethod.equals("GET") || reqMethod.equals("POST")) {
            super.service(req, resp);
        } else {
            resp.setStatus(405); // Method Not Allowed
        }
    }

    private class Invoker {
        final Object obj;
        final Method method;

        private Invoker(Object obj, Method method) {
            this.obj = obj;
            this.method = method;
        }

        public void invoke(HttpServletRequest req, HttpServletResponse resp) throws IOException {
//            L.trace("begin invoke");
        	QueryParams qp = QueryParams.create(req);
//        	L.trace("end query params create");
            try {
                Class[] pts = method.getParameterTypes();
                Object[] ps = new Object[pts.length];
                for (int i = 0; i < pts.length; i++) {
                    Class pt = pts[i];
                    if (pt.isAssignableFrom(QueryParams.class)) {
                        ps[i] = qp;
                    } else if (pt.isAssignableFrom(HttpServletRequest.class)) {
                        ps[i] = req;
                    } else if (pt.isAssignableFrom(HttpServletResponse.class)) {
                        ps[i] = resp;
                    }
                }

                Throwable err = null;
                Object r = null;
                try {
                    method.setAccessible(true);
//                    L.trace("begin invoke method");
                	r = method.invoke(obj, ps);
//                    L.trace("end invoke method");
                } catch (InvocationTargetException e) {
                    err = e.getTargetException();
                } catch (Throwable t) {
                    err = t;
                }

                if (err != null) {
                    writeError(err, qp, req, resp, printErrorDetails);
                } else {
                    if (r instanceof NoResponse) {
                        // do nothing
                    } else if (r instanceof DirectResponse) {
                        DirectResponse dr = (DirectResponse) r;
                        output(qp, req, resp, dr.content, 200, dr.contentType);
                    } else {
                        String json = JsonUtils.toJson(wrapSingleton(r), true);
//                        output(resp, json, 200, "application/json");
                        output(qp, req, resp, json, 200, "text/plain");
                    }
                }
            } finally {
                if (qp != null)
                    qp.close();
            }
//            L.trace("end invoke");
        }
    }

    private static Object wrapSingleton(Object o) {
        JsonNodeFactory jnf = JsonNodeFactory.instance;
        Object r;
        if (o == null || o instanceof Null) {
            r = makeResultObjectNode(jnf, jnf.nullNode());
        } else if (o instanceof Privacy || o instanceof CharSequence || o instanceof Character) {
            r = makeResultObjectNode(jnf, jnf.textNode(o.toString()));
        } else if (o instanceof Boolean) {
            r = makeResultObjectNode(jnf, jnf.booleanNode((Boolean) o));
        } else if (o instanceof Byte || o instanceof Short || o instanceof Integer || o instanceof Long) {
            r = makeResultObjectNode(jnf, jnf.numberNode(((Number) o).longValue()));
        } else if (o instanceof Float || o instanceof Double) {
            r = makeResultObjectNode(jnf, jnf.numberNode(((Number) o).doubleValue()));
        } else {
            r = o;
        }
        return r;
    }

    private static JsonNode makeResultObjectNode(JsonNodeFactory jnf, JsonNode jn) {
        ObjectNode r = jnf.objectNode();
        r.put("result", jn);
        return r;
    }


    private static void writeError(final Throwable err, QueryParams qp, HttpServletRequest req, HttpServletResponse resp, boolean printErrorDetails) throws IOException {
        if (err instanceof ResponseError || err instanceof ServerException || err instanceof AvroRuntimeException) {
            final ResponseError respErr = Errors.wrapResponseError(err);
            String json = JsonUtils.toJson(new JsonGenerateHandler() {
                @Override
                public void generate(JsonGenerator jg) throws IOException {
                    jg.writeStartObject();
                    jg.writeNumberField("error_code", respErr.code);
                    jg.writeStringField("error_msg", ObjectUtils.toString(respErr.message, ""));
                    jg.writeEndObject();
                }
            }, true);
//            output(resp, json, 200, "application/json");
            output(qp, req, resp, json, 200, "text/plain");
        } else {
            if (printErrorDetails) {
                StringWriter out = new StringWriter();
                out.append("Call stack:\n");
                err.printStackTrace(new PrintWriter(out));
                String errorStack = out.toString();
                output(qp, req, resp, errorStack, 500, "text/plain");
            } else {
                String json = JsonUtils.toJson(new JsonGenerateHandler() {
                    @Override
                    public void generate(JsonGenerator jg) throws IOException {
                        jg.writeStartObject();
                        jg.writeNumberField("error_code", ErrorCode.UNKNOWN_ERROR);
                        jg.writeStringField("error_msg", ObjectUtils.toString(err.getMessage(), ""));
                        jg.writeEndObject();
                    }
                }, true);
//                output(resp, json, 500, "application/json");
                output(qp, req, resp, json, 500, "text/plain");
            }
        }


    }
}
