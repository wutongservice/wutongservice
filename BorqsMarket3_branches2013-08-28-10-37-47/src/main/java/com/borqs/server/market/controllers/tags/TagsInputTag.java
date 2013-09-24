package com.borqs.server.market.controllers.tags;


import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.JsonUtils;
import com.borqs.server.market.utils.StringUtils2;
import com.borqs.server.market.utils.i18n.SpringMessage;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.jsp.PageContext;
import java.util.Map;

public class TagsInputTag extends TextInputTag {
    private String typeAhead = "";
    private String typeAheadUrl = "";
    private boolean allowFreeTags = false;

    public TagsInputTag() {
        super("TagsInputTag.ftl");
    }

    public String getTypeAhead() {
        return typeAhead;
    }

    public void setTypeAhead(String typeAhead) {
        this.typeAhead = typeAhead;
    }

    public String getTypeAheadUrl() {
        return typeAheadUrl;
    }

    public void setTypeAheadUrl(String typeAheadUrl) {
        this.typeAheadUrl = typeAheadUrl;
    }

    public boolean isAllowFreeTags() {
        return allowFreeTags;
    }

    public void setAllowFreeTags(boolean allowFreeTags) {
        this.allowFreeTags = allowFreeTags;
    }

    private static String makeTypeAheadJson(String typeAhead) {
        return JsonUtils.toJson(StringUtils2.splitSet(ObjectUtils.toString(typeAhead), ',', true), false);
    }

    private String formatTip(String typeAhead, boolean allowFreeTags) {
        PageContext pageCtx = (PageContext) getJspContext();
        if (StringUtils.isNotBlank(typeAhead)) {
            String[] availableTags = StringUtils2.splitArray(ObjectUtils.toString(typeAhead), ',', true);
            if (allowFreeTags) {
                return String.format(SpringMessage.get("TagsInputTag.text.availableTagsWithOthers", pageCtx),
                        StringUtils.join(availableTags, ","));
            } else {
                return String.format(SpringMessage.get("TagsInputTag.text.availableTags", pageCtx),
                        StringUtils.join(availableTags, ","));
            }
        } else {
            return "";
        }
    }

    @Override
    protected Map<String, Object> getData() {
        Map<String, Object> data = super.getData();
        data.put("typeAhead", makeTypeAheadJson(typeAhead));
        data.put("typeAheadUrl", typeAhead);
        data.put("value", makeTypeAheadJson(value));
        data.put("allowFreeTags", allowFreeTags);
        data.put("availableTagsTip", formatTip(typeAhead, allowFreeTags));
        return data;

    }
}
