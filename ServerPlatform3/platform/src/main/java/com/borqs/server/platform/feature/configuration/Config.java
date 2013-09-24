package com.borqs.server.platform.feature.configuration;

import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.util.ColumnsExpander;
import com.borqs.server.platform.util.ObjectHelper;
import com.borqs.server.platform.util.json.JsonBean;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Config extends Addons implements JsonBean {

    public static final String COL_USER_ID = "user_id";
    public static final String COL_CONFIG_KEY = "config_key";
    public static final String COL_VALUE = "value";
    public static final String COL_VERSION_CODE = "version_code";
    public static final String COL_CONTENT_TYPE = "content_type";
    public static final String COL_CREATED_TIME = "created_time";

    private long userId;
    private String configKey;
    private long createdTime;
    private String value;
    private int versionCode;
    private int contentType;


    public Config() {
    }



    public static final String[] STANDARD_COLUMNS = {
            COL_VALUE,
            COL_CONFIG_KEY,
            COL_VERSION_CODE,
            COL_CONTENT_TYPE,
            COL_USER_ID,
            COL_CONTENT_TYPE,
            COL_CREATED_TIME,
    };
    public static final String[] FULL_COLUMNS = {
            COL_VALUE,
            COL_CONFIG_KEY,
            COL_VERSION_CODE,
            COL_CONTENT_TYPE,
            COL_USER_ID,
            COL_CONTENT_TYPE,
            COL_CREATED_TIME,

    };


    private static Map<String, String[]> columnAliases = new ConcurrentHashMap<String, String[]>();

    static {
        registerColumnsAlias("@std,#std", STANDARD_COLUMNS);
        registerColumnsAlias("@full,#full", FULL_COLUMNS);
    }

    public static String[] expandColumns(String[] cols) {
        return ColumnsExpander.expand(cols, columnAliases);
    }

    public static void registerColumnsAlias(String alias, String[] cols) {
        columnAliases.put(alias, cols);
    }

    public static void unregisterColumnsAlias(String alias) {
        columnAliases.remove(alias);
    }


    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }


    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public int getContentType() {
        return this.contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }



    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }



    public static Map<String, String[]> getColumnAliases() {
        return columnAliases;
    }

    public static void setColumnAliases(Map<String, String[]> columnAliases) {
        Config.columnAliases = columnAliases;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Config other = (Config) o;
        return createdTime == other.createdTime
                && StringUtils.equals(configKey, other.configKey);

    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(createdTime,
                configKey);
    }

    @Override
    public void deserialize(JsonNode jn) {
        if (jn.has(COL_CREATED_TIME))
            setCreatedTime(jn.path(COL_CREATED_TIME).getValueAsLong());
        if (jn.has(COL_VALUE))
            setValue(jn.path(COL_VALUE).getValueAsText());
        if (jn.has(COL_VERSION_CODE))
            setVersionCode(jn.path(COL_VERSION_CODE).getValueAsInt());
        if (jn.has(COL_CONTENT_TYPE))
            setVersionCode(jn.path(COL_CONTENT_TYPE).getValueAsInt());
        if (jn.has(COL_USER_ID))
            setUserId(jn.path(COL_USER_ID).getValueAsLong());
        if (jn.has(COL_CONTENT_TYPE))
            setContentType(jn.path(COL_CONTENT_TYPE).getValueAsInt());



    }

    public void serialize(JsonGenerator jg, String[] cols) throws IOException {
        jg.writeStartObject();

        if (outputColumn(cols, COL_CREATED_TIME))
            jg.writeNumberField(COL_CREATED_TIME, getCreatedTime());

        if (outputColumn(cols, COL_VALUE))
            jg.writeStringField(COL_VALUE, getValue());
        if (outputColumn(cols, COL_VERSION_CODE))
            jg.writeNumberField(COL_VERSION_CODE, getVersionCode());
        if (outputColumn(cols, COL_CONTENT_TYPE))
            jg.writeNumberField(COL_CONTENT_TYPE, getVersionCode());
        if (outputColumn(cols, COL_CONTENT_TYPE))
            jg.writeNumberField(COL_CONTENT_TYPE, getContentType());
        if (outputColumn(cols, COL_USER_ID))
            jg.writeNumberField(COL_USER_ID, getUserId());

        writeAddonsJson(jg, cols);
        jg.writeEndObject();
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException {
        serialize(jg, (String[]) null);
    }

    public String toJson(final String[] cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serialize(jg, cols);
            }
        }, human);
    }


    public static Config fromJsonNode(JsonNode jn) {
        Config config = new Config();
        config.deserialize(jn);
        return config;
    }

    public static Config fromJson(String json) {
        return fromJsonNode(JsonHelper.parse(json));
    }

    @Override
    public String toString() {
        return toJson(null, true);
        //return super.toString();
    }

}
