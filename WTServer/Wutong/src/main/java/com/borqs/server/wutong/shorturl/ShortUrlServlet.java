package com.borqs.server.wutong.shorturl;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.template.PageTemplate;
import com.borqs.server.base.web.webmethod.NoResponse;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.email.template.InnovTemplate;
import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ShortUrlServlet extends WebMethodServlet {

    private static final PageTemplate pageTemplate = new PageTemplate(InnovTemplate.class);
    private String serverHost;

    @Override
    public void init() throws ServletException {
        super.init();
        Configuration conf = getConfiguration();
        serverHost = conf.getString("server.host", "api.borqs.com");

    }

    @WebMethod("link/longurl")
    public NoResponse getLongUrl(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String param = qp.checkGetString("short_url");
        param = StringUtils.substringBefore(param, "\\");
        if (!param.toUpperCase().startsWith("HTTP://"))
            param = "http://" + param;
        String long_url = GlobalLogics.getShortUrl().getLongUrl(param);

        if (StringUtils.contains(long_url, "bpc.borqs.com")) {
            long_url = StringUtils.substringBefore(long_url, "?generate_time");
        }
        if (long_url.equals("http://"+serverHost+"/link/expired")) {
            String html = pageTemplate.merge("notice.ftl", new Object[][]{
                    {"host", serverHost},
                    {"notice", "您的请求已过期"}
            });
            resp.setContentType("text/html");
            resp.getWriter().print(html);

            return NoResponse.get();
        }

        try {
            resp.sendRedirect(long_url);
            return null;
        } catch (IOException e) {
            throw new ServerException(WutongErrors.SYSTEM_HTTP_METHOD_NOT_SUPPORT, "url send redirect error");
        }
    }
}
