package com.borqs.server.market.controllers.tags;


import com.borqs.server.market.controllers.Attributes;
import com.borqs.server.market.service.ServiceConsts;
import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppNavigationBarTag extends AbstractFreemarkerJspTag {

    private String module;
    private String currentAppId;

    public AppNavigationBarTag() {
        super("AppNavigationBarTag.ftl");
    }

    public String getCurrentAppId() {
        return currentAppId;
    }

    public void setCurrentAppId(String currentAppId) {
        this.currentAppId = currentAppId;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    @Override
    protected Map<String, Object> getData() {
        PageContext pageContext = (PageContext) getJspContext();
        HttpServletRequest req = (HttpServletRequest) pageContext.getRequest();
        Records allApps = Attributes.getAllApps(req);
        List<NavigationItem> items = getItems(module, allApps);
        return CC.map(
                "currentAppId=>", currentAppId != null ? currentAppId : Attributes.getCurrentAppId(req),
                "items=>", items
        );
    }

    private List<NavigationItem> getItems(String module, Records apps) {
        ArrayList<NavigationItem> items = new ArrayList<NavigationItem>();
        if (ServiceConsts.MODULE_PUBLISH.equalsIgnoreCase(module)) {
            for (Record app : apps)
                items.add(createAppItem(app, ServiceConsts.MODULE_PUBLISH));
        } else if (ServiceConsts.MODULE_OPER.equalsIgnoreCase(module)) {
            for (Record app : apps)
                items.add(createAppItem(app, ServiceConsts.MODULE_OPER));
        }
        return items;
    }

    public static NavigationItem createAppItem(Record app, String module) {
        String appId = app.asString("id");
        return new NavigationItem(appId, app.asString("name"), "/" + module + "/apps/" + appId);
    }
}
