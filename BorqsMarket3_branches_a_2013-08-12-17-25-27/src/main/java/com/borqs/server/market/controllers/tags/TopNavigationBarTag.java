package com.borqs.server.market.controllers.tags;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.controllers.Attributes;
import com.borqs.server.market.service.ServiceConsts;
import com.borqs.server.market.utils.CC;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import java.util.Map;

public class TopNavigationBarTag extends AbstractFreemarkerJspTag {
    private String module;

    public TopNavigationBarTag() {
        super("TopNavigationBarTag.ftl");
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    @Override
    protected Map<String, Object> getData() {
        ServiceContext ctx = Attributes.getServiceContext((HttpServletRequest) ((PageContext) getJspContext()).getRequest());
        int roles = ctx != null ? ctx.getRoles() : ServiceConsts.ROLE_PURCHASER;
        return CC.map(
                "module=>", module,
                "developEnabled=>", (roles & ServiceConsts.ROLE_DEVELOPER) != 0,
                "operEnabled=>", (roles & ServiceConsts.ROLE_OPERATOR) != 0,
                "gstatEnabled=>", (roles & ServiceConsts.ROLE_BOSS) != 0
        );
    }
}
