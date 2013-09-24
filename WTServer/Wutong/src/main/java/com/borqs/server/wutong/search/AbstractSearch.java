package com.borqs.server.wutong.search;


import org.apache.commons.lang.ObjectUtils;

public abstract class AbstractSearch implements SearchLogic {
    protected AbstractSearch() {
    }

    protected static String trimQueryWord(String q) {
        return ObjectUtils.toString(q);
    }
}
