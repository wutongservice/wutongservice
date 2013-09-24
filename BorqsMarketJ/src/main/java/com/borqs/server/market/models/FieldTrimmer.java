package com.borqs.server.market.models;


import com.borqs.server.market.utils.record.Record;
import org.codehaus.jackson.JsonNode;

import java.util.Iterator;
import java.util.List;

public class FieldTrimmer {

    private static void selectFieldLanguage(Record rec, String field, String lang) {
        Object val = rec.get(field);
        if (val instanceof JsonNode) {
            JsonNode langs = (JsonNode) val;
            if (langs.isObject()) {
                String text = "";
                if (langs.has(lang)) {
                    text = langs.path(lang).asText();
                } else {
                    String defLang = rec.asString("default_lang", null);
                    if (defLang != null) {
                        text = langs.path(defLang).asText();
                    } else {
                        if (langs.has("en_US")) {
                            text = langs.path("en_US").asText();
                        } else{
                            int langCount = langs.size();
                            if (langCount == 1) {
                                Iterator<String> langNameIter = langs.getFieldNames();
                                while (langNameIter.hasNext()) {
                                    String langName = langNameIter.next();
                                    text = langs.path(langName).asText();
                                }
                            }
                        }
                    }
                }
                rec.put(field, text);
            }
        }
    }

    private static final String[] LANGS_FIELDS = {
            "name", "version_name", "description", "recent_change", "update_change",
            "app_name", "category_name",
    };

    public FieldTrimmer trimLanguage(Record rec, String lang) {
        for (String field : LANGS_FIELDS) {
            selectFieldLanguage(rec, field, lang);
        }
        return this;
    }

    public FieldTrimmer trimPrice(Record rec, String lang, boolean removeFree) {
        if (rec.hasField("price") && rec.hasField("free")) {
            if (rec.asBoolean("free", false)) {
                rec.set("price", "0");
            } else {
                String price;
                JsonNode prices = rec.getJsonNode("price", null);
                if (prices.has(lang)) {
                    price = prices.path(lang).asText();
                } else {
                    price = prices.has("default") ? prices.path("default").asText() : "?";
                }
                rec.set("price", price);
            }
        }

        if (removeFree) {
            rec.removeField("free");
        }
        return this;
    }

    public FieldTrimmer trimUrlField(Record rec, String field) {
        // TODO: xx
        return this;
    }



    public FieldTrimmer trimLanguage(List<? extends Record> recs, String lang) {
        for (Record rec : recs) {
            if (rec != null)
                trimLanguage(rec, lang);
        }
        return this;
    }

    public FieldTrimmer trimPrice(List<? extends Record> recs, String lang, boolean removeFree) {
        for (Record rec : recs) {
            if (rec != null)
                trimPrice(rec, lang, removeFree);
        }
        return this;
    }

}
