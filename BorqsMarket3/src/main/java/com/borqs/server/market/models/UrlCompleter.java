package com.borqs.server.market.models;


import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import com.borqs.server.market.utils.record.Record;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import java.util.List;

public class UrlCompleter {
    private String imagePrefix;
    private String productPrefix;

    public UrlCompleter() {
    }


    public String getImagePrefix() {
        return imagePrefix;
    }

    public void setImagePrefix(String imagePrefix) {
        this.imagePrefix = imagePrefix;
    }

    public String getProductPrefix() {
        return productPrefix;
    }

    public void setProductPrefix(String productPrefix) {
        this.productPrefix = productPrefix;
    }

    private static String addPrefix(String url, String prefix) {
        if (url.isEmpty())
            return url;

        if (!StringUtils.startsWithIgnoreCase(url, "http:")
                && !StringUtils.startsWithIgnoreCase(url, "https:")) {
            return StringUtils.removeEnd(prefix, "/") + "/" + StringUtils.removeStart(url, "/");
        } else {
            return url;
        }
    }

    public String completeImageUrl(String imageUrl) {
        return addPrefix(imageUrl, imagePrefix);
    }

    public void completeUrlField(Record rec, String field, String prefix) {
        if (rec == null)
            return;

        if (rec.hasField(field)) {
            Object val = rec.get(field);
            if (val instanceof String) {
                rec.put(field, addPrefix((String) val, prefix));
            } else if (val instanceof JsonNode && ((JsonNode) val).isObject()) {
                ObjectNode objNode = (ObjectNode) val;
                if (objNode.has("url")) {
                    String url = objNode.path("url").asText();
                    url = addPrefix(url, prefix);
                    objNode.put("url", url);
                }
            }
        }
    }


    private static final String[] IMAGE_URL_FIELDS = {
            "logo_image",
            "cover_image",
            "promotion_image",
            "screenshot1_image",
            "screenshot2_image",
            "screenshot3_image",
            "screenshot4_image",
            "screenshot5_image",
    };

    private static final String[] PRODUCT_URL_FIELD = {
            "url",
    };

    public void completeUrl(Record rec) {
        for (String field : IMAGE_URL_FIELDS)
            completeUrlField(rec, field, imagePrefix);

        for (String field : PRODUCT_URL_FIELD)
            completeUrlField(rec, field, productPrefix);
    }

    public void completeUrl(List<Record> recs) {
        for (Record rec : recs) {
            if (rec != null)
                completeUrl(rec);
        }
    }

    public void completeUrl(RecordsWithTotal recs) {
        completeUrl(recs.getRecords());
    }
}
