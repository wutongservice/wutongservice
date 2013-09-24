package com.borqs.server.market.utils.mybatis.typehandlers;


import com.borqs.server.market.utils.JsonUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(JsonNode.class)
public class JsonHandler extends AbstractStringHandler<JsonNode> {
    public static final int DEFAULT_OBJECT = 1;
    public static final int DEFAULT_ARRAY = 2;

    private int defaultValue = DEFAULT_OBJECT;

    public JsonHandler() {
    }

    public int getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(int defaultValue) {
        Validate.isTrue(defaultValue == DEFAULT_OBJECT || defaultValue == DEFAULT_ARRAY);
        this.defaultValue = defaultValue;
    }

    @Override
    protected String toString(JsonNode val) throws Exception {
        return val != null ? JsonUtils.toJson(val, false) : null;
    }

    private JsonNode makeDefaultValue() {
        if (defaultValue == DEFAULT_ARRAY) {
            return JsonNodeFactory.instance.arrayNode();
        } else if (defaultValue == DEFAULT_OBJECT) {
            return JsonNodeFactory.instance.objectNode();
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    protected JsonNode fromString(String s) throws Exception {
        if (s == null) {
            return null;
        } else if (StringUtils.isBlank(s)) {
            return makeDefaultValue();
        } else {
            return JsonUtils.parseJson(s);
        }
    }
}
