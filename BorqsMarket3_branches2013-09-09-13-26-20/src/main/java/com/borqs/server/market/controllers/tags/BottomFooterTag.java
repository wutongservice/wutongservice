package com.borqs.server.market.controllers.tags;


import java.util.HashMap;
import java.util.Map;

public class BottomFooterTag extends AbstractFreemarkerJspTag {
    public BottomFooterTag() {
        super("BottomFooterTag.ftl");
    }

    @Override
    protected Map<String, Object> getData() {
        return new HashMap<String, Object>();
    }
}
