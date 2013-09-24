package com.borqs.server.market.models;


import org.apache.commons.lang.StringUtils;

public class ProductIds {
    public static boolean productIdIsUserShared(String productId) {
        return StringUtils.isEmpty(productId) || StringUtils.startsWith(productId, "USP_");
    }
}
