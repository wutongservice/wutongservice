package com.borqs.server.wutong.action.actions.holiday;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.template.PageTemplate;
import com.borqs.server.base.web.webmethod.NoResponse;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.account2.util.json.JsonHelper;
import com.borqs.server.wutong.commons.WutongContext;
import com.borqs.server.wutong.email.template.InnovTemplate;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HoliActionServlet extends WebMethodServlet {
    private String serverHost;
    private static final PageTemplate pageTemplate = new PageTemplate(InnovTemplate.class);

    @Override
    public void init() throws ServletException {
        super.init();
        Configuration conf = getConfiguration();
        serverHost = conf.getString("server.host", "api.borqs.com");
    }


    @WebMethod("actionHoliday/Consumer")
    public void actionHoliday1(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Context ctx = WutongContext.getContext(qp, false);

        String json = qp.checkGetString("jn");
        ActionsBizLogic holiday = GlobalLogics.getHolidayBiz();
        JsonNode jn = JsonHelper.parse(json);
        holiday.consumer(ctx, jn);

    }

    @WebMethod("actionHoliday/holiday")
    public NoResponse actionHoliday(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Context ctx = WutongContext.getContext(qp, true);
        String post_id = qp.checkGetString("post_id");
        String type = qp.checkGetString("type");

        Record r = Record.of("post_id", post_id, "type", type);
        ActionsBizLogic holiday = GlobalLogics.getHolidayBiz();
        String lang = ctx.getLanguage();

        try {
            holiday.callBack(ctx, r);
        } catch (Exception e) {
            String msg = e.getMessage();
            String notice = StringUtils.isBlank(msg) ? Constants.getBundleStringByLang(lang, "platformservlet.create.action.failed") : msg;
            String html = pageTemplate.merge("notice.ftl", new Object[][]{
                    {"host", serverHost},
                    {"notice", notice}
            });
            resp.setContentType("text/html");
            resp.getWriter().print(html);
        }

        String notice = Constants.getBundleStringByLang(lang, "platformservlet.create.action.success");
        String html = pageTemplate.merge("notice.ftl", new Object[][]{
                {"host", serverHost},
                {"notice", notice}
        });

        resp.setContentType("text/html");
        resp.getWriter().print(html);

        return NoResponse.get();

    }
}
