package com.borqs.server.wutong.setting;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.template.PageTemplate;
import com.borqs.server.base.web.webmethod.NoResponse;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.commons.WutongContext;
import com.borqs.server.wutong.nuser.setting.NUserSettingLogic;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class SettingServlet extends WebMethodServlet {
    private static final PageTemplate pageTemplate = new PageTemplate(SettingServlet.class);
    private static String serverHost;

    @Override
    public void init() throws ServletException {
        super.init();
        Configuration conf = getConfiguration();
        serverHost = conf.getString("server.host", "api.borqs.com");
    }

    @WebMethod("preferences/set")
    public boolean setPreferences(QueryParams qp) throws AvroRemoteException {
        Context ctx = WutongContext.getContext(qp, false);
        String viewerId = ctx.getViewerIdString();

        Record values = new Record();
        Iterator iter = qp.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            String[] buildInParams = new String[]{"sign_method", "sign", "appid", "ticket"};
            if (!ArrayUtils.contains(buildInParams, key)) {
                values.put(key, value);
            }
        }

        return GlobalLogics.getSetting().set(ctx, viewerId, values);
    }

    @WebMethod("preferences/subscribe")
    public NoResponse subscribeEmail(QueryParams qp, HttpServletResponse resp) throws IOException {
        Context ctx = WutongContext.getContext(qp, false);
        String user = qp.checkGetString("user");
        String type = qp.checkGetString("type");
        String value = qp.getString("value", "0");

        SettingLogic setting = GlobalLogics.getSetting();
        NUserSettingLogic nUsersetting = GlobalLogics.getNewUsersetting();
        AccountLogic account = GlobalLogics.getAccount();

        String userId = account.findUserIdByUserName(ctx, user);
        boolean isUser = StringUtils.isNotBlank(userId) && !StringUtils.equals(userId, "0");

        Record values = Record.of(type, value);
        boolean r = isUser ? setting.set(ctx, userId, values) : nUsersetting.set(ctx,user, values);

        String opt = StringUtils.equals(value, "0") ? "订阅" : "退订";
        String notice = r ? opt + "成功！" : opt + "失败，请稍候再试。";
        String html = pageTemplate.merge("notice.freemarker", new Object[][]{
                {"host", serverHost},
                {"notice", notice}
        });

        resp.setContentType("text/html");
        resp.getWriter().print(html);

        return NoResponse.get();
    }

    @WebMethod("preferences/get")
    public Record getPreferences(QueryParams qp) throws AvroRemoteException {
        Context ctx = WutongContext.getContext(qp, false);
        String viewerId = ctx.getViewerIdString();
        SettingLogic setting = GlobalLogics.getSetting();

        return setting.gets(ctx,viewerId, qp.checkGetString("keys"));
    }

    @WebMethod("preferences/get_by_starts")
    public Record getPreferencesByStarts(QueryParams qp) throws AvroRemoteException {
        Context ctx = WutongContext.getContext(qp, false);
        String viewerId = ctx.getViewerIdString();
        SettingLogic setting = GlobalLogics.getSetting();

        return setting.getsByStartsWith(ctx,viewerId, qp.checkGetString("starts"));
    }

    @WebMethod("preferences/get_by_users")
    public Record getPreferencesByUsers(QueryParams qp) throws AvroRemoteException {
        Context ctx = WutongContext.getContext(qp, false);
        String viewerId = ctx.getViewerIdString();
        SettingLogic setting = GlobalLogics.getSetting();

        return setting.getByUsers(ctx,qp.checkGetString("key"), qp.checkGetString("users"));
    }
}
