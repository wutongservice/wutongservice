package com.borqs.server.wutong.appsettings;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.commons.WutongContext;

import javax.servlet.ServletException;

public class AppSettingServlet extends WebMethodServlet {
    private String serverHost;

    @Override
    public void init() throws ServletException {
        super.init();
        Configuration conf = getConfiguration();
        serverHost = conf.getString("server.host", "api.borqs.com");
    }

    @WebMethod("appsetting/set")
    public Record setAppSettingConfigs(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        String key = qp.checkGetString("key");
        int version = (int) qp.getInt("version", 0);
        String value = qp.checkGetString("value");
        String description = qp.getString("description", "");

        AppSettingLogic actionLogic = GlobalLogics.getAppSetting();
        Record record = new Record()
                .set("id", Long.toString(RandomUtils.generateId()))
                .set("user_id", ctx.getViewerIdString())
                .set("key_", key)
                .set("version", version)
                .set("value_", value)
                .set("description", description)
                .set("created_time", DateUtils.nowMillis())
                .set("destroyed_time", 0);
        return actionLogic.setSetting(ctx, record);
    }

    @WebMethod("appsetting/get")
    public RecordSet getAppSettingConfigs(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        String key = qp.checkGetString("key");
        int version = (int) qp.getInt("version", 0);

        AppSettingLogic actionLogic = GlobalLogics.getAppSetting();
        return actionLogic.getSettings(ctx, key, version);
    }

}
