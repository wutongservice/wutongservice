package com.borqs.server.market.models;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import com.borqs.server.market.utils.record.Record;
import org.apache.commons.lang.ObjectUtils;
import org.codehaus.jackson.JsonNode;

import java.util.List;

public class LocaleSelector {

    private String defaultLocale = "en_US";

    public LocaleSelector() {
    }

    public LocaleSelector(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    private JsonNode findDefaultLocaleNode(Record rec, JsonNode localeVals) {
        JsonNode localeNode = localeVals.get("default");
        if (localeNode == null) {
            String defaultLocale = rec.asString("default_locale", null);
            if (defaultLocale == null) {
                defaultLocale = this.defaultLocale;
            }
            localeNode = localeVals.get(defaultLocale);
        }
        return localeNode;
    }

    public Object selectLocale(Object val, String locale) {
        Record rec = new Record().set("val", val);
        selectLocaleField(rec, "val", locale);
        return rec.get("val");
    }

    public String selectLocaleForText(Object val, String locale) {
        val = selectLocale(val, locale);
        return ObjectUtils.toString(val);
    }

    public void selectLocaleField(Record rec, String field, String locale) {
        if (rec == null)
            return;

        Object val = rec.get(field);
        if (val instanceof JsonNode) {
            JsonNode localeVals = (JsonNode) val;
            if (localeVals.isObject() && !localeVals.has("cs")) {
                JsonNode localeNode;
                if (locale != null) {
                    localeNode = localeVals.get(locale);
                    if (localeNode == null)
                        localeNode = findDefaultLocaleNode(rec, localeVals);
                } else {
                    localeNode = findDefaultLocaleNode(rec, localeVals);
                }

                if (localeNode == null)
                    throw new RuntimeException("Can't find locale text");

                Object localeVal = localeNode.isTextual() ? localeNode.asText() : localeNode;
                rec.put(field, localeVal);
            }
        }
    }

    private static final String[] LOCALIZABLE_FIELDS = {
            "name",
            "version_name",
            "description",
            "recent_change",
            "update_change",
            "app_name",
            "category_name",
            "price",
            "cmcc_mm_price",
            "amazon_price",
    };


    public void selectLocale(Record rec, String locale) {
        for (String field : LOCALIZABLE_FIELDS)
            selectLocaleField(rec, field, locale);
    }

    private static String getContextLocale(ServiceContext ctx) {
        return ctx != null ? ctx.getClientLocale() : "en_US";
    }

    public void selectLocale(Record rec, ServiceContext ctx) {
        selectLocale(rec, getContextLocale(ctx));
    }

    public void selectLocale(List<Record> recs, String locale) {
        for (Record rec : recs) {
            if (rec != null)
                selectLocale(rec, locale);
        }
    }

    public void selectLocale(List<Record> recs, ServiceContext ctx) {
        selectLocale(recs, getContextLocale(ctx));
    }

    public void selectLocale(RecordsWithTotal recs, String locale) {
        selectLocale(recs.getRecords(), locale);
    }

    public void selectLocale(RecordsWithTotal recs, ServiceContext ctx) {
        selectLocale(recs, getContextLocale(ctx));
    }
}
