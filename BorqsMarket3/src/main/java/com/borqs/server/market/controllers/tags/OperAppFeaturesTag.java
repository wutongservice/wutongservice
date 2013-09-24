package com.borqs.server.market.controllers.tags;


import com.borqs.server.market.utils.CC;
import org.apache.commons.lang.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OperAppFeaturesTag extends AbstractFreemarkerJspTag {

    public static final String FEATURE_MANAGE_PARTITIONS = "managePartitions";
    public static final String FEATURE_MANAGE_PROMOTIONS = "managePromotions";

    private String currentCategory;
    private String currentFeature;
    private String appId;

    public OperAppFeaturesTag() {
        super("OperAppFeaturesTag.ftl");
    }

    public String getCurrentCategory() {
        return currentCategory;
    }

    public void setCurrentCategory(String currentCategory) {
        this.currentCategory = currentCategory;
    }

    public String getCurrentFeature() {
        return currentFeature;
    }

    public void setCurrentFeature(String currentFeature) {
        this.currentFeature = currentFeature;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    @Override
    protected Map<String, Object> getData() {
        List<NavigationItem> featureItems = createFeatureItems();
        return CC.map(
                "currentFeature=>", currentFeature != null ? currentFeature : FEATURE_MANAGE_PARTITIONS,
                "featureItems=>", featureItems
        );
    }

    private List<NavigationItem> createFeatureItems() {
        ArrayList<NavigationItem> featureItems = new ArrayList<NavigationItem>();
        featureItems.add(new NavigationItem(
                FEATURE_MANAGE_PARTITIONS,
                "OperAppFeaturesTag.label.oper.managePartitions",
                "/oper/apps/" + appId + "/partitions?category=" + ObjectUtils.toString(currentCategory)));
        featureItems.add(new NavigationItem(
                FEATURE_MANAGE_PROMOTIONS,
                "OperAppFeaturesTag.label.oper.managePromotions",
                "/oper/apps/" + appId + "/promotions?category=" + ObjectUtils.toString(currentCategory)));
        return featureItems;
    }
}
