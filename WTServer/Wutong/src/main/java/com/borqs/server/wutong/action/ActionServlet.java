package com.borqs.server.wutong.action;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.commons.WutongContext;

import javax.servlet.ServletException;

public class ActionServlet extends WebMethodServlet {
    private String serverHost;

    @Override
    public void init() throws ServletException {
        super.init();
        Configuration conf = getConfiguration();
        serverHost = conf.getString("server.host", "api.borqs.com");
    }

    @WebMethod("action/getconfig")
    public RecordSet getActionConfigs(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        String scope = qp.checkGetString("scope");
        String name = qp.getString("name", "");

        ActionLogic actionLogic = GlobalLogics.getAction();
        return actionLogic.getActionConfig(ctx, scope, name);
    }

}
