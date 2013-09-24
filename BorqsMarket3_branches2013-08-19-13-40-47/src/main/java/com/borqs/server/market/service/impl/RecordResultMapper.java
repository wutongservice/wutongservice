package com.borqs.server.market.service.impl;


import com.borqs.server.market.utils.CurrencyUtils;
import com.borqs.server.market.utils.JsonUtils;
import com.borqs.server.market.utils.PrimitiveTypeConverter;
import com.borqs.server.market.utils.StringUtils2;
import com.borqs.server.market.utils.mybatis.record.RecordMapper;
import com.borqs.server.market.utils.record.Record;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class RecordResultMapper implements RecordMapper {

    private static final RecordResultMapper instance = new RecordResultMapper();

    private RecordResultMapper() {

    }

    public static RecordResultMapper get() {
        return instance;
    }

    @Override
    public Record map(Record rec) {
        if (rec == null)
            return null;

        rec.transactValue("name", jsonTransactor);
        rec.transactValue("version_name", jsonTransactor);
        rec.transactValue("description", jsonTransactor);
        rec.transactValue("recent_change", jsonTransactor);
        //rec.transactValue("dependencies", jsonTransactor);
        rec.transactValue("price", priceTransactor);
        rec.transactValue("category_name", jsonTransactor);
        rec.transactValue("app_name", jsonTransactor);
        rec.transactValue("supported_mod", arrayTransactor);
        for (String field : rec.getFields()) {
            if (field.startsWith("is_") || field.startsWith("has_")) {
                Object b = rec.get(field);
                rec.put(field, PrimitiveTypeConverter.toBoolean(b));
            }
        }
        return rec;
    }


    private static final Record.ValueTransactor jsonTransactor = new Record.ValueTransactor() {
        @Override
        public Object transact(String field, Object val) {
            try {
                if (val instanceof JsonNode) {
                    return val;
                } else {
                    String s = ObjectUtils.toString(val);
                    if (StringUtils.isBlank(s)) {
                        return JsonNodeFactory.instance.objectNode();
                    } else {
                        return JsonUtils.parseJson(s);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    };

    private static final Record.ValueTransactor arrayTransactor = new Record.ValueTransactor() {
        @Override
        public Object transact(String field, Object val) {
            if (val instanceof String[]) {
                return val;
            } else {
                String s = ObjectUtils.toString(val);
                return StringUtils2.splitArray(s, ',', true);
            }
        }
    };

    private static final Record.ValueTransactor priceTransactor = new Record.ValueTransactor() {
        @Override
        public Object transact(String field, Object val) {
            if (val == null)
                return null;

            String priceStr = ObjectUtils.toString(val);
            if (StringUtils.isBlank(priceStr) || priceStr.equals("0"))
                return null;

            JsonNode pricesNode = (JsonNode) jsonTransactor.transact(field, priceStr);
            if (pricesNode != null && pricesNode.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> iter = pricesNode.getFields();
                while (iter.hasNext()) {
                    JsonNode priceNode = iter.next().getValue();
                    if (priceNode != null && priceNode.isObject()) {
                        String display = CurrencyUtils.toDisplay(priceNode.path("cs").asText(), priceNode.path("amount").asDouble(0.0));
                        ((ObjectNode) priceNode).put("display", display);
                    }
                }
            }

            return pricesNode;
        }
    };
}
