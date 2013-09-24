package com.borqs.server.platform.test;


import com.borqs.server.platform.web.GlobalApplicationContextDelegateServlet;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.servletunit.ServletRunner;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

public abstract class ServletTestCase extends ConfigurableTestCase {

    public static final String TEST_HOST = "http://test.user.com";


    public static final String UA_EMPTY = "";
    public static final String UA_CHROME_18 = "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.152 Safari/535.19";
    public static final String UA_SAFARI_3 = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en) AppleWebKit/522.11.3 (KHTML, like Gecko) Version/3.0 Safari/522.11.3";
    public static final String UA_IE_6 = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; SV1; .NET CLR 1.1.4322)";
    public static final String UA_OPERA_9 = "Opera/9.0 (Windows NT 5.0; U; en)";
    public static final String UA_FIREFOX_2 = "Mozilla/5.0 (Windows; U; Windows NT 5.2; en-GB; rv:1.8.1.18) Gecko/20081029 Firefox/2.0.0.18";

    static {
        HttpUnitOptions.setScriptingEnabled(false);
        HttpUnitOptions.setExceptionsThrownOnErrorStatus(false);
        HttpUnitOptions.setExceptionsThrownOnScriptError(false);
    }

    private static final String PARAM_SERVLET_TEST_CASE = "servletTestCase";

    protected ServletRunner servletRunner;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setupServletRunner();
    }

    @Override
    public void tearDown() throws Exception {
        teardownServletRunner();
        super.tearDown();
    }

    protected void setupServletRunner() {
        teardownServletRunner();

        servletRunner = new ServletRunner();
        String[] servletBeanIds = getServletBeanIds();

        if (ArrayUtils.isNotEmpty(servletBeanIds)) {
            for (String beanId : servletBeanIds) {
                Hashtable<String, Object> initParams = new Hashtable<String, Object>();
                initParams.put("bean", beanId);
                servletRunner.registerServlet(beanId + "/*", GlobalApplicationContextDelegateServlet.class.getName(), initParams);
                servletRunner.registerServlet("/*", getMissingResourceServletClassName());
            }
        }
    }

    protected void teardownServletRunner() {
        if (servletRunner != null) {
            servletRunner.shutDown();
            servletRunner = null;
        }
    }

    protected abstract String[] getServletBeanIds();

    protected TestHttpClient newHttpClient() {
        return newHttpClient(null);
    }

    protected TestHttpClient newHttpClient(Map<String, String> headers) {
        if (MapUtils.isEmpty(headers))
            return new TestHttpClient(servletRunner.newClient(), TEST_HOST);
        else
            return new TestHttpClient(servletRunner.newClient(), TEST_HOST, headers);
    }

    protected TestHttpApiClient newHttpApiClient(String userAgent) {
        return newHttpApiClient(userAgent, null, null, null);
    }

    protected TestHttpApiClient newHttpApiClient(String userAgent, String ticket) {
        return newHttpApiClient(userAgent, ticket, null, null);
    }

    protected TestHttpApiClient newHttpApiClient(String userAgent, int appId, String appSecret) {
        return newHttpApiClient(userAgent, null, appId, appSecret);
    }

    protected TestHttpApiClient newHttpApiClient(String userAgent, String ticket, Integer appId, String appSecret) {
        TestHttpApiClient apiClient = new TestHttpApiClient(servletRunner.newClient());
        apiClient.setHost(TEST_HOST);
        if (userAgent != null)
            apiClient.setUserAgent(userAgent);
        if (ticket != null)
            apiClient.setTicket(ticket);
        if (appId != null)
            apiClient.setAppId(appId);
        if (appSecret != null)
            apiClient.setAppSecret(appSecret);
        return apiClient;
    }

    protected String getMissingResourceServletClassName() {
        return MissingResourceServlet.class.getName();
    }

    public static class MissingResourceServlet extends HttpServlet {

        protected void missingResource(HttpServletRequest req, HttpServletResponse resp) {
            resp.setStatus(404);
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            missingResource(req, resp);
        }

        @Override
        protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            missingResource(req, resp);
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            missingResource(req, resp);
        }

        @Override
        protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            missingResource(req, resp);
        }

        @Override
        protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            missingResource(req, resp);
        }

        @Override
        protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            missingResource(req, resp);
        }

        @Override
        protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            missingResource(req, resp);
        }
    }
}
