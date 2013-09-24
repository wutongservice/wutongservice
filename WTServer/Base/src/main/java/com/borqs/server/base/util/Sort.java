package com.borqs.server.base.util;

import org.apache.commons.lang.StringUtils;

public class Sort {
    public String orderBy;
    public final String ascOrDesc;

    private Sort(String orderBy, String ascOrDesc) {
        this.orderBy = orderBy;
        this.ascOrDesc = ascOrDesc;
    }

    public static Sort parse(String sort) {
        if (sort.startsWith("-")) {
            String orderBy = StringUtils.removeStart(sort, "-");
            return new Sort(StringUtils.lowerCase(orderBy), "DESC");
        } else {
            String orderBy = StringUtils.removeStart(sort, "+");
            return new Sort(StringUtils.lowerCase(orderBy), "ASC");
        }
    }
}
