package com.borqs.server.market.controllers.tags;


import com.borqs.server.market.controllers.Attributes;
import com.borqs.server.market.service.ServiceConsts;
import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.StringUtils2;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SecondNavigationBarTag extends AbstractFreemarkerJspTag {

    private String module;
    private String currentAt;

    public SecondNavigationBarTag() {
        super("SecondNavigationBarTag.ftl");
    }

    public String getCurrentAt() {
        return currentAt;
    }

    public void setCurrentAt(String currentAt) {
        this.currentAt = currentAt;
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
        Records allProducts = Attributes.getAllProducts(req);
        List<Level> levels = getLevels(module, currentAt, allApps, allProducts);
        return CC.map("levels=>", levels);
    }

    private List<Level> getLevels(String module, String currentAt, Records apps, Records products) {
        ArrayList<Level> levels = new ArrayList<Level>();
        if (ServiceConsts.MODULE_PUBLISH.equalsIgnoreCase(module)) {
            String[] segs = StringUtils2.splitArray(currentAt, "/", true);
            if (segs.length > 0 && "apps".equalsIgnoreCase(segs[0])) {
                levels.add(new Level(new NavigationItem(null, "Apps", "/publish/welcome")));
            }
            if (segs.length > 1) {
                String appId = segs[1];
                NavigationItem titleItem = getAppItem(apps, appId, ServiceConsts.MODULE_PUBLISH);
                levels.add(new Level(titleItem));
            }
            if (segs.length > 2) {
                String productId = segs[2];
                NavigationItem titleItem = getProductItem(products, productId);
                if (titleItem != NavigationItem.NULL) {
                    levels.add(new Level(titleItem, getProductsItems(products)));
                }
            }
        } else if (ServiceConsts.MODULE_OPER.equalsIgnoreCase(module)) {
            String[] segs = StringUtils2.splitArray(currentAt, "/", true);
            if (segs.length > 0 && "apps".equalsIgnoreCase(segs[0])) {
                levels.add(new Level(new NavigationItem(null, "Apps", "/oper/welcome")));
            }
            if (segs.length > 1) {
                String appId = segs[1];
                NavigationItem titleItem = getAppItem(apps, appId, ServiceConsts.MODULE_OPER);
                levels.add(new Level(titleItem));
            }
        } else if (ServiceConsts.MODULE_PUBLISH.equalsIgnoreCase(module)) {
            // TODO: ...
        } else if (ServiceConsts.MODULE_GSTAT.equalsIgnoreCase(module)) {
            String[] segs = StringUtils2.splitArray(currentAt, "/", true);
            if (segs.length > 0) {
                String type = segs[0];
                if ("all".equalsIgnoreCase(type)) {
                    levels.add(new Level(new NavigationItem(null, "All", "/gstat/all")));
                }
            }
        }
        return levels;
    }

    private static NavigationItem getAppItem(Records apps, String appId, String module) {
        for (Record app : apps) {
            if (appId.equals(app.asString("id")))
                return AppNavigationBarTag.createAppItem(app, module);
        }
        return NavigationItem.NULL;
    }

    private static NavigationItem getProductItem(Records products, String productId) {
        for (Record product : products) {
            if (productId.equals(product.asString("id")))
                return createProductItem(product);
        }
        return NavigationItem.NULL;
    }

    private static List<NavigationItem> getProductsItems(Records products) {
        ArrayList<NavigationItem> items = new ArrayList<NavigationItem>();
        for (Record product : products) {
            items.add(createProductItem(product));
        }
        return items;
    }

    public static NavigationItem createProductItem(Record product) {
        String id = product.asString("id");
        String name = product.asString("name");
        String link = "/publish/products/" + id;
        return new NavigationItem(id, name, link);
    }

    public static class Level {
        private NavigationItem titleItem;
        private List<NavigationItem> altItems;

        public Level(NavigationItem titleItem) {
            this(titleItem, new ArrayList<NavigationItem>());
        }

        public Level(NavigationItem titleItem, List<NavigationItem> altItems) {
            this.titleItem = titleItem;
            this.altItems = altItems;
        }

        public NavigationItem getTitleItem() {
            return titleItem;
        }

        public List<NavigationItem> getAltItems() {
            return altItems;
        }
    }
}
