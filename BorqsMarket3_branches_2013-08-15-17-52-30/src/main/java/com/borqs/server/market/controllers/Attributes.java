package com.borqs.server.market.controllers;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.utils.record.Records;

import javax.servlet.http.HttpServletRequest;

public class Attributes {

    public static final String SERVICE_CONTEXT = "_serviceContext_";
    public static final String ALL_APPS = "allApps";
    public static final String ALL_PRODUCTS = "allProducts";
    public static final String CURRENT_APP_ID = "currentAppId";
    public static final String CURRENT_MODULE = "currentModule";

    static void setAttributeValue(HttpServletRequest req, String name, Object val) {
        if (val != null) {
            req.setAttribute(name, val);
        } else {
            req.removeAttribute(name);
        }
    }

    public static void setServiceContext(HttpServletRequest req, ServiceContext ctx) {
        setAttributeValue(req, SERVICE_CONTEXT, ctx);
    }

    public static ServiceContext getServiceContext(HttpServletRequest req) {
        return (ServiceContext) req.getAttribute(SERVICE_CONTEXT);
    }

    public static void setCurrentAppId(HttpServletRequest req, String appId) {
        setAttributeValue(req, CURRENT_APP_ID, appId);
    }

    public static String getCurrentAppId(HttpServletRequest req) {
        return (String) req.getAttribute(CURRENT_APP_ID);
    }

    public static void setCurrentModule(HttpServletRequest req, String module) {
        setAttributeValue(req, CURRENT_MODULE, module);
    }

    public static String getCurrentModule(HttpServletRequest req) {
        return (String) req.getAttribute(CURRENT_MODULE);
    }

    public static void setAllApps(HttpServletRequest req, Records apps) {
        setAttributeValue(req, ALL_APPS, apps);
    }

    public static Records getAllApps(HttpServletRequest req) {
        return (Records) req.getAttribute(ALL_APPS);
    }

    public static void setAllProducts(HttpServletRequest req, Records products) {
        setAttributeValue(req, ALL_PRODUCTS, products);
    }

    public static Records getAllProducts(HttpServletRequest req) {
        return (Records) req.getAttribute(ALL_PRODUCTS);
    }


}
