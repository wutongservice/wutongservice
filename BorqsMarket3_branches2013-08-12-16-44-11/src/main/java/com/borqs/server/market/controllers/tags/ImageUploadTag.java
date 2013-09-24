package com.borqs.server.market.controllers.tags;


import com.borqs.server.market.utils.CC;

import java.util.Map;

public class ImageUploadTag extends AbstractFreemarkerJspTag {
    private String style = "";
    private String id = "";
    private String alt = "";
    private String src = "";


    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public ImageUploadTag() {
        super("ImageUploadTag.ftl");
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    @Override
    protected Map<String, Object> getData() {
        return CC.map("style=>", style,
                "id=>", id,
                "alt=>", alt,
                "src=>", src
        );
    }
}
