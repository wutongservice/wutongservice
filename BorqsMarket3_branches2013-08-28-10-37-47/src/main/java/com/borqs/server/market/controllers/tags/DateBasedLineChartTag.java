package com.borqs.server.market.controllers.tags;


import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.JsonUtils;
import com.borqs.server.market.utils.StringUtils2;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class DateBasedLineChartTag extends AbstractChartTag {

    private boolean autoYKeys = true;
    private String namedYKeys = "";

    public DateBasedLineChartTag() {
        super("DateBasedLineChartTag.ftl");
    }

    public boolean isAutoYKeys() {
        return autoYKeys;
    }

    public void setAutoYKeys(boolean autoYKeys) {
        this.autoYKeys = autoYKeys;
    }

    public String getNamedYKeys() {
        return namedYKeys;
    }

    public void setNamedYKeys(String namedYKeys) {
        this.namedYKeys = namedYKeys;
    }

    private Map<String, String> getYKeys(Records data) {
        LinkedHashMap<String, String> yKeys = new LinkedHashMap<String, String>();
        if (autoYKeys && data != null) {
            for (Record rec : data) {
                for (String k : rec.keySet()) {
                    if (!"dates".equalsIgnoreCase(k))
                        yKeys.put(k, k);
                }
            }
        }
        String[] ss = StringUtils2.splitArray(ObjectUtils.toString(namedYKeys), ",", true);
        for (String nyk : ss) {
            String yk = StringUtils.substringBefore(nyk, "=>").trim();
            String ykLabel = StringUtils.substringAfter(nyk, "=>").trim();
            if (yKeys.containsKey(yk))
                yKeys.put(yk, ykLabel);
        }

        return yKeys;
    }

    @Override
    protected Map<String, Object> getData() {
        Map<String, String> yKeysMap = getYKeys(graphData);
        ArrayList<String> yKeys = new ArrayList<String>();
        ArrayList<String> labels = new ArrayList<String>();
        for (Map.Entry<String, String> entry : yKeysMap.entrySet()) {
            yKeys.add(entry.getKey());
            labels.add(entry.getValue());
        }
        return CC.map(
                "id=>", ObjectUtils.toString(id),
                "styleClass=>", ObjectUtils.toString(styleClass),
                "style=>", ObjectUtils.toString(style),
                "dataJson=>", graphData != null ? JsonUtils.toJson(graphData, true) : "[]",
                "yKeysJson=>", JsonUtils.toJson(yKeys, false),
                "labelsJson=>", JsonUtils.toJson(labels, false)
        );
    }
}
