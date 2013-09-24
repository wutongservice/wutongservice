package com.borqs.server.base.web.webmethod;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import com.borqs.server.base.ResponseError;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.data.Null;
import com.borqs.server.base.data.Privacy;
import com.borqs.server.base.io.Charsets;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.ElapsedCounter;
import com.borqs.server.base.util.ErrorUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.util.json.JsonGenerateHandler;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.base.web.OutputServlet;
import com.borqs.server.base.web.QueryParams;
import org.apache.avro.AvroRuntimeException;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;


public class WebMethodServlet extends OutputServlet {
    private static final Logger L = Logger.getLogger(WebMethodServlet.class);

    public static final String ELAPSED_COUNTER_ATTR = "$_elapsed_counter_";
    public static final String ELAPSED_COUNTER_HEADER = "Enable-Elapsed-Counter";

    private final Map<String, Invoker> invokers = new HashMap<String, Invoker>();
    private volatile Configuration configuration;
    private boolean printErrorDetails = false;
    private ConnectionFactory connectionFactory = ConnectionFactory.getConnectionFactory("dbcp");
    private String db;

    private static int alreadyFetchedPhotoURLHostConfig = -1;
    private static String platform_photoUrlPattern      = "";

    public WebMethodServlet() {
        initMethods();
    }

    protected String getPhotoURLHostConfiguration()
    {
        if(alreadyFetchedPhotoURLHostConfig == -1 || StringUtils.isEmpty(platform_photoUrlPattern))
        {
            alreadyFetchedPhotoURLHostConfig = 0;
            platform_photoUrlPattern = getConfiguration().checkGetString("platform.photoUrlPattern");
        }

        return platform_photoUrlPattern;
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
                throw new ServerException(BaseErrors.PLATFORM_WEBMETHOD_INIT_ERROR, "Repetitive url '%s'", url);

            for (Class pt : method.getParameterTypes()) {
                if (!pt.isAssignableFrom(HttpServletRequest.class) &&
                        !pt.equals(HttpServletResponse.class) &&
                        !pt.equals(QueryParams.class))
                    throw new ServerException(BaseErrors.PLATFORM_WEBMETHOD_INIT_ERROR, "Invalid parameter type '%s'", pt.getName());
            }

            invokers.put(url, new Invoker(this, method));
        }
    }

    public Configuration getConfiguration() {
//        if (configuration == null) {
//            Configuration conf = new Configuration();
//            Enumeration e = getInitParameterNames();
//            while (e.hasMoreElements()) {
//                String key = e.nextElement().toString();
//                String val = getInitParameter(key);
//                conf.put(key, val);
//            }
//            configuration = conf;
//        }
//        return configuration;

        return GlobalConfig.get();
    }

    @Override
    public void init() throws ServletException {
        super.init();
        Configuration conf = getConfiguration();
        printErrorDetails = conf.getBoolean("webMethod.printErrorDetails", true);  // TODO: default should false
        db = conf.getString("platform.simple.db", null);
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

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type,Authorization,Accept,X-Requested-With,User-Agent,location");
        resp.setHeader("Access-Control-Allow-Credentials", "false");
        resp.setHeader("Access-Control-Max-Age", "60");
    }

    protected boolean before(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        L.trace("begin before");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setCharacterEncoding(Charsets.DEFAULT);
        String ua = req.getHeader("User-Agent");
        L.trace(null, "----" + StringUtils.trimToEmpty(ua));
        if (StringUtils.isNotBlank(ua) && StringUtils.containsIgnoreCase(ua, "Sogou web spider")) {
//            L.trace("end before");
            return false;
        }
        if (StringUtils.isNotBlank(ua) && StringUtils.containsIgnoreCase(ua, "alisec")) {
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

    private void recordCall(String api, long callAt) {
        try {
            final String SQL1 = "INSERT INTO api_counter (api, call_count, call_elapsed, last_call_time) " +
                    " VALUES (${v(api)}, 1, ${v(elapsed)}, ${v(call_at)}) " +
                    " ON DUPLICATE KEY UPDATE call_count=call_count+1, call_elapsed=(call_elapsed * call_count + ${v(elapsed)}) / (call_count + 1), last_call_time=${v(call_at)}";

            final String SQL2 = "INSERT INTO api_counter (api, call_count, call_elapsed, last_call_time) " +
                    " VALUES (${v(api)}, 1, 0, ${v(call_at)}) " +
                    " ON DUPLICATE KEY UPDATE call_count=call_count+1, last_call_time=${v(call_at)}";
            long elapsed = DateUtils.nowMillis() - callAt;
            if (elapsed > 0) {
                String sql = SQLTemplate.merge(SQL1,
                        "api", api, "elapsed", elapsed, "call_at", callAt);
                SQLExecutor.executeUpdate(connectionFactory, db, sql);
            } else {
                String sql = SQLTemplate.merge(SQL2,
                        "api", api, "call_at", callAt);
                SQLExecutor.executeUpdate(connectionFactory, db, sql);
            }
        } catch (Exception e) {
            L.warn(null, e, "record api call error");
        }
    }

    private void doProcess(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        L.traceHttpReq(req);
        req.setAttribute(ELAPSED_COUNTER_ATTR, new ElapsedCounter());
        long callAt = DateUtils.nowMillis();
        try {
            boolean b = before(req, resp);
            if (b) {
                process(req, resp);
            }
        } finally {
            after(req, resp);
            recordCall(req.getRequestURI(), callAt);

            //TODO
            //statics call user-agent
        }
    }


    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String reqMethod = req.getMethod();
        if (reqMethod.equals("GET") || reqMethod.equals("POST") || reqMethod.equals("OPTIONS")) {
            super.service(req, resp);
        } else {
            resp.setStatus(405); // Method Not Allowed
        }
    }

    private static boolean isElapsedCounterEnabled(HttpServletRequest req) {
        return StringUtils.equals(req.getHeader(ELAPSED_COUNTER_HEADER), "1");
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
                    r = method.invoke(obj, ps);
                } catch (InvocationTargetException e) {
                    err = e.getTargetException();
                } catch (Throwable t) {
                    err = t;
                }

                if (isElapsedCounterEnabled(req)) {
                    ElapsedCounter ec = (ElapsedCounter) req.getAttribute(ELAPSED_COUNTER_ATTR);
                    String summary = (ec == null || ec.isEmpty()) ? "NO Summary" : StringUtils.join(ec.toStringArray(), "\r\n");
                    resp.setContentType("text/plain");
                    resp.getWriter().print(summary);
                } else {
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
        String errorStack0 = null;
        if (printErrorDetails) {
            StringWriter out = new StringWriter();
            out.append("Call stack:\n");
            err.printStackTrace(new PrintWriter(out));
            errorStack0 = out.toString();
        }
        final String errorStack = errorStack0;


        if (err instanceof ResponseError || err instanceof ServerException || err instanceof AvroRuntimeException) {
            final ServerException respErr = ErrorUtils.wrapResponseError(err);
            String json = JsonUtils.toJson(new JsonGenerateHandler() {
                @Override
                public void generate(JsonGenerator jg) throws IOException {
                    jg.writeStartObject();
                    jg.writeNumberField("error_code", respErr.code);
                    jg.writeStringField("error_msg", ObjectUtils.toString(respErr.getMessage(), ""));
                    if (errorStack != null)
                        jg.writeStringField("error_stack", errorStack);
                    jg.writeEndObject();
                }
            }, true);
//            output(resp, json, 200, "application/json");
            output(qp, req, resp, json, 200, "text/plain");
        } else {

            String json = JsonUtils.toJson(new JsonGenerateHandler() {
                @Override
                public void generate(JsonGenerator jg) throws IOException {
                    jg.writeStartObject();
                    jg.writeNumberField("error_code", BaseErrors.PLATFORM_UNKNOWN_ERROR);
                    jg.writeStringField("error_msg", ObjectUtils.toString(err.getMessage(), ""));
                    if (errorStack != null)
                        jg.writeStringField("error_stack", errorStack);
                    jg.writeEndObject();
                }
            }, true);
//                output(resp, json, 500, "application/json");
            output(qp, req, resp, json, 500, "text/plain");

        }


    }
}
