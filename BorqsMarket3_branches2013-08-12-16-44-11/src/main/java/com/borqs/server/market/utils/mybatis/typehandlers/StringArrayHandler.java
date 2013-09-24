package com.borqs.server.market.utils.mybatis.typehandlers;

import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.type.MappedTypes;


@MappedTypes(String[].class)
public class StringArrayHandler extends AbstractStringHandler<String[]> {
    public StringArrayHandler() {
    }

    @Override
    protected String toString(String[] val) throws Exception {
        return val != null ? StringUtils.join(val, ',') : null;
    }

    @Override
    protected String[] fromString(String s) throws Exception {
        return s != null ? StringUtils.split(s, ',') : null;
    }
}
