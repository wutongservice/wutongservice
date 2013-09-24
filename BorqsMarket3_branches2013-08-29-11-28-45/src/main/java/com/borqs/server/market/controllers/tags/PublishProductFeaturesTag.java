package com.borqs.server.market.controllers.tags;


import com.borqs.server.market.utils.CC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PublishProductFeaturesTag extends AbstractFreemarkerJspTag {

    public static final String FEATURE_INFO = "info";
    public static final String FEATURE_STAT = "stat";
    public static final String FEATURE_ORDERS = "orders";
    public static final String FEATURE_COMMENTS = "comments";

    private String currentFeature;
    private String productId;
    private Integer version;

    public PublishProductFeaturesTag() {
        super("PublishProductFeaturesTag.ftl");
    }

    public String getCurrentFeature() {
        return currentFeature;
    }

    public void setCurrentFeature(String currentFeature) {
        this.currentFeature = currentFeature;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    protected Map<String, Object> getData() {
        List<NavigationItem> featureItems = createFeatureItems();
        return CC.map(
                "featureItems=>", featureItems,
                "currentFeature=>", currentFeature != null ? currentFeature : FEATURE_INFO
        );
    }

    private List<NavigationItem> createFeatureItems() {
        ArrayList<NavigationItem> featureItems = new ArrayList<NavigationItem>();
        String infoLink;
        if (version != null && version > 0) {
            infoLink = "/publish/products/" + productId + "/" + version;
        } else {
            infoLink = "/publish/products/" + productId;
        }

        featureItems.add(new NavigationItem(FEATURE_INFO, "PublishProductFeaturesTag.label.publish.info", infoLink));
        featureItems.add(new NavigationItem(FEATURE_STAT, "PublishProductFeaturesTag.label.publish.stat", "/publish/products/" + productId + "/stat"));
        featureItems.add(new NavigationItem(FEATURE_ORDERS, "PublishProductFeaturesTag.label.publish.orders", "/publish/products/" + productId + "/orders"));
        featureItems.add(new NavigationItem(FEATURE_COMMENTS, "PublishProductFeaturesTag.label.publish.comments", "/publish/products/" + productId + "/comments"));
        return featureItems;
    }
}
