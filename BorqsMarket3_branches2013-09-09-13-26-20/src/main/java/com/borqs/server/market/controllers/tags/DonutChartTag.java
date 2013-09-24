package com.borqs.server.market.controllers.tags;


import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.JsonUtils;
import org.apache.commons.lang.ObjectUtils;

import java.util.Map;

public class DonutChartTag extends AbstractChartTag {

    public DonutChartTag() {
        super("DonutChartTag.ftl");
    }

    @Override
    protected Map<String, Object> getData() {
        return CC.map(
                "id=>", ObjectUtils.toString(id),
                "styleClass=>", ObjectUtils.toString(styleClass),
                "style=>", ObjectUtils.toString(style),
                "dataJson=>", graphData != null ? JsonUtils.toJson(graphData, true) : "[]"
        );
    }
}
