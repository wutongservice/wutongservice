package com.borqs.server.platform.web.topaz;

import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.util.Initializable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopazServlet extends HttpServlet implements Initializable {
    private static final Logger L = Logger.get(TopazServlet.class);

    private final List<MatchEntity> before = new ArrayList<MatchEntity>();
    private final List<MatchEntity> after = new ArrayList<MatchEntity>();
    private final List<MatchEntity> routes = new ArrayList<MatchEntity>();


    private List<Object> handlers;

    protected boolean errorDetail = false;
    protected boolean outputCompress = false;
    protected boolean routeSummary = false;
    protected List<AccessHook> accessHooks;


    protected TopazServlet() {
    }

    private void addRoutes(Object handler) {
        Class clazz = handler.getClass();
        for (Method method : clazz.getMethods()) {
            int modifier = method.getModifiers();
            if (!(Modifier.isPublic(modifier)))
                continue;

            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 2 || !paramTypes[0].equals(Request.class) || !paramTypes[1].equals(Response.class))
                continue;

            Before beforeAnn = method.getAnnotation(Before.class);
            After afterAnn = method.getAnnotation(After.class);
            Route routeAnn = method.getAnnotation(Route.class);

            if (routeAnn != null)
                routes.add(new MatchEntity(handler, method, routeAnn));
            else if (beforeAnn != null)
                before.add(new MatchEntity(handler, method, beforeAnn));
            else if (afterAnn != null)
                after.add(new MatchEntity(handler, method, afterAnn));
        }

        if (L.isInfoEnabled()) {
            for (MatchEntity e : routes)
                L.info(null, "route " + ObjectUtils.toString(e));
            for (MatchEntity e : before)
                L.info(null, "before " + ObjectUtils.toString(e));
            for (MatchEntity e : after)
                L.info(null, "after " + ObjectUtils.toString(e));
        }
    }

    public List<Object> getHandlers() {
        return handlers;
    }

    public void setHandlers(List<Object> handlers) {
        this.handlers = handlers;
    }

    public List<AccessHook> getAccessHooks() {
        return accessHooks;
    }

    public void setAccessHooks(List<AccessHook> accessHooks) {
        this.accessHooks = accessHooks;
    }

    public boolean isOutputCompress() {
        return outputCompress;
    }

    public void setOutputCompress(boolean outputCompress) {
        this.outputCompress = outputCompress;
    }

    public boolean isErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(boolean errorDetail) {
        this.errorDetail = errorDetail;
    }

    public boolean isRouteSummary() {
        return routeSummary;
    }

    public void setRouteSummary(boolean routeSummary) {
        this.routeSummary = routeSummary;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        if (CollectionUtils.isEmpty(handlers))
            return;

        for (Object handler : handlers)
            addRoutes(handler);
    }

    @Override
    protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    protected Request createRequest(HttpServletRequest hreq, HttpServletResponse hresp) {
        return new Request(hreq);
    }

    protected Response createResponse(HttpServletRequest hreq, HttpServletResponse hresp) {
        Response resp = new Response(hresp);
        resp.setErrorDetail(errorDetail);
        resp.setGzip(outputCompress);
        return resp;
    }


    private static interface AccessHookCallback {
        void callHook(AccessHook hook);
    }

    protected void fireAccessHook(AccessHookCallback callback) {
        List<AccessHook> hooks = accessHooks;
        if (CollectionUtils.isEmpty(hooks))
            return;

        for (AccessHook hook : hooks) {
            if (hook != null)
                callback.callHook(hook);
        }
    }

    protected void fireBeforeAll(final Request req, final Response resp) {
        fireAccessHook(new AccessHookCallback() {
            @Override
            public void callHook(AccessHook hook) {
                hook.beforeAll(req, resp);
            }
        });
    }

    protected void fireExceptionCaught(final Request req, final Response resp, final Throwable t) {
        try {
            fireAccessHook(new AccessHookCallback() {
                @Override
                public void callHook(AccessHook hook) {
                    hook.exceptionCaught(req, resp, t);
                }
            });
        } catch (Exception e) {
            L.warn(null, e, "fireExceptionCaught error");
        }
    }

    protected void fireSuccess(final Request req, final Response resp) {
        fireAccessHook(new AccessHookCallback() {
            @Override
            public void callHook(AccessHook hook) {
                hook.success(req, resp);
            }
        });
    }

    protected void fireBeforeOutput(final Request req, final Response resp) {
        try {
            fireAccessHook(new AccessHookCallback() {
                @Override
                public void callHook(AccessHook hook) {
                    hook.beforeOutput(req, resp);
                }
            });
        } catch (Exception e) {
            L.warn(null, e, "fireBeforeOutput error");
        }
    }

    protected void fireAfterOutput(final Request req, final Response resp) {
        try {
            fireAccessHook(new AccessHookCallback() {
                @Override
                public void callHook(AccessHook hook) {
                    hook.afterOutput(req, resp);
                }
            });
        } catch (Exception e) {
            L.warn(null, e, "fireAfterOutput error");
        }
    }

    protected void fireAfterAll(final Request req, final Response resp) {
        try {
            fireAccessHook(new AccessHookCallback() {
                @Override
                public void callHook(AccessHook hook) {
                    hook.afterAll(req, resp);
                }
            });
        } catch (Exception e) {
            L.warn(null, e, "fireAfterAll error");
        }
    }


    protected void process(HttpServletRequest hreq, HttpServletResponse hresp) throws ServletException, IOException {
        Request req = createRequest(hreq, hresp);
        if (L.isDebugEnabled())
            L.debug(null, req.toString());

        Response resp = createResponse(hreq, hresp);
        try {
            fireBeforeAll(req, resp);
            if (routeSummary && "$".equals(StringUtils.removeStart(StringUtils.trimToEmpty(hreq.getPathInfo()), "/"))) {
                resp.body(RawText.of(makeRouteSummary()));
                resp.type("text/plain");
            } else {
                invokeAllMatched(before, req, resp);
                boolean matched = invokeFirstMatched(routes, req, resp);
                invokeAllMatched(after, req, resp);
                if (!matched)
                    TopazHelper.halt(404, "Route error");

                if (L.isDebugEnabled())
                    L.debug(null, "ok");
            }
            fireSuccess(req, resp);
        } catch (Throwable t) {
            fireExceptionCaught(req, resp, t);
            Throwable c = (t instanceof InvocationTargetException) ? ((InvocationTargetException) t).getTargetException() : t;
            if (!(c instanceof QuietHaltException))
                resp.error(c);

            L.warn(null, c, "error");
        } finally {
            fireBeforeOutput(req, resp);
            resp.doOutput(Response.OutputOptions.fromRequest(req));
            fireAfterOutput(req, resp);
            req.deleteUploadedFiles();
            fireAfterAll(req, resp);
            if (L.isDebugEnabled())
                L.debug(null, "complete");
        }
    }



    private String makeRouteSummary() {
        StringBuilder buff = new StringBuilder();
        for (MatchEntity route : routes) {
            buff.append(route.toRouteString()).append("\n");
        }
        return buff.toString();
    }

    private static boolean invokeFirstMatched(List<MatchEntity> matchEntities, Request req, Response resp) throws InvocationTargetException, IllegalAccessException {
        for (MatchEntity matchEntity : matchEntities) {
            if (matchEntity == null)
                continue;

            if (matchEntity.match(req)) {
                matchEntity.invoke(req, resp);
                return true;
            }
        }
        return false;
    }

    private static boolean invokeAllMatched(List<MatchEntity> matchEntities, Request req, Response resp) throws InvocationTargetException, IllegalAccessException {
        boolean matched = false;
        for (MatchEntity matchEntity : matchEntities) {
            if (matchEntity == null)
                continue;

            if (matchEntity.match(req)) {
                matchEntity.invoke(req, resp);
                matched = true;
            }
        }
        return matched;
    }

    private static class MatchEntity {
        final Object instance;
        final Method method;
        final String[] urlPatterns;
        final String[] httpMethods;

        private MatchEntity(Object instance, Method method, String[] urlPatterns, String[] httpMethods) {
            this.instance = instance;
            this.method = method;
            this.urlPatterns = urlPatterns;
            this.httpMethods = httpMethods;
        }

        private MatchEntity(Object instance, Method method, Route route) {
            this(instance, method, route.url(), route.method());
        }

        private MatchEntity(Object instance, Method method, Before before) {
            this(instance, method, before.url(), before.method());
        }

        private MatchEntity(Object instance, Method method, After after) {
            this(instance, method, after.url(), after.method());
        }

        private static boolean match(String patt, String s, Request req) {
            HashMap<String, String> m = new HashMap<String, String>();
            if (!UrlMatcher.match(patt, s, m))
                return false;

            for (Map.Entry<String, String> e : m.entrySet())
                req.set(e.getKey(), e.getValue());

            return true;
        }

        public boolean match(Request req) {
            HttpServletRequest hreq = req.httpRequest;
            boolean matched = false;

            // HTTP method
            String reqMethod = req.httpRequest.getMethod();
            for (String httpMethod : httpMethods) {
                if (StringUtils.equalsIgnoreCase(httpMethod, reqMethod)) {
                    matched = true;
                    break;
                }
            }
            if (!matched)
                return false;

            // URL
            matched = false;
            //String url = StringHelper.joinIgnoreNull(hreq.getServletPath(), hreq.getPathInfo());
            String url = StringUtils.trimToEmpty(hreq.getPathInfo());
            if (!url.startsWith("/"))
                url = "/" + url;

            for (String urlPatt : urlPatterns) {
                if (match(urlPatt, url, req)) {
                    matched = true;
                    break;
                }
            }

            return matched;
        }

        public void invoke(Request req, Response resp) throws InvocationTargetException, IllegalAccessException {
            method.invoke(instance, req, resp);
        }

        @Override
        public String toString() {
            StringBuilder buff = new StringBuilder();
            buff.append(StringUtils.join(httpMethods, "|"));
            buff.append("  ").append(StringUtils.join(urlPatterns, "|"));
            buff.append(" => ");
            buff.append(instance.getClass().getName()).append(".").append(method.getName());
            return buff.toString();
        }

        public String toRouteString() {
            StringBuilder buff = new StringBuilder();
            buff.append(StringUtils.join(httpMethods, "|"));
            buff.append("  ").append(StringUtils.join(urlPatterns, "|"));
            return buff.toString();
        }
    }
}
