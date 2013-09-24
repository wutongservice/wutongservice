package com.borqs.server.market.controllers.tags;


import com.borqs.server.market.utils.CC;

import java.util.Map;

public class TextAreaInputTag extends TextInputTag {

    protected int rows = 3;

    public TextAreaInputTag() {
        super("TextAreaInputTag.ftl");
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    @Override
    protected Map<String, Object> getData() {
        Map<String, Object> data = super.getData();
        data.put("rows", rows);
        return data;
    }
}
