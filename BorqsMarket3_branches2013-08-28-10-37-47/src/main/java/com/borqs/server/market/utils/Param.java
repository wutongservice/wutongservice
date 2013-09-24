package com.borqs.server.market.utils;


import org.apache.commons.fileupload.FileItem;
import org.codehaus.jackson.JsonNode;
import org.springframework.util.StringUtils;

import java.io.IOException;

public class Param {
    public static final Param NON_EXISTS = new Param(null);

    public final Object value;

    public Param(Object value) {
        this.value = value;
    }

    public boolean exists() {
        return this != NON_EXISTS;
    }

    public String asString(String def) {
        if (exists()) {
            if (value instanceof JsonNode) {
                return JsonUtils.toJson(value, false);
            } else {
                return PrimitiveTypeConverter.toStr(value);
            }
        } else {
            return def;
        }
    }

    public String asString() {
        return asString(null);
    }

    public boolean asBoolean(boolean def) {
        return exists() ? PrimitiveTypeConverter.toBoolean(value, def) : def;
    }

    public int asInt(int def) {
        return exists() ? PrimitiveTypeConverter.toInt(value, def) : def;
    }

    public int asInt() {
        return asInt(0);
    }

    public Integer asIntObject() {
        return exists() ? PrimitiveTypeConverter.toInt(value) : null;
    }

    public long asLong(long def) {
        return exists() ? PrimitiveTypeConverter.toLong(value, def) : def;
    }

    public long asLong() {
        return asLong(0L);
    }

    public Long asLongObject() {
        return exists() ? PrimitiveTypeConverter.toLong(value) : null;
    }

    public double asDouble(double def) {
        return exists() ? PrimitiveTypeConverter.toDouble(value, def) : def;
    }

    public double asDouble() {
        return asDouble(0.0);
    }

    public JsonNode asJson() {
        if (exists()) {
            if (value instanceof JsonNode) {
                return (JsonNode) value;
            } else {
                String json = asString(null);
                try {
                    return StringUtils.isEmpty(json)
                            ? null
                            : JsonUtils.parseJson(json);
                } catch (IOException e) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    public String[] asArray(String sep, boolean trim, String[] def) {
        String s = asString(null);
        return s != null ? StringUtils2.splitArray(s, sep, trim) : def;
    }

    public String[] asArray(String sep, boolean trim) {
        return asArray(sep, trim, null);
    }

    public int[] asIntArray(String sep, int[] def) {
        String s = asString(null);
        return s != null ? StringUtils2.splitIntArray(s, sep) : def;
    }

    public int[] asIntArray(String sep) {
        return asIntArray(sep, null);
    }

    public long[] asLongArray(String sep, long[] def) {
        String s = asString(null);
        return s != null ? StringUtils2.splitLongArray(s, sep) : def;
    }

    public long[] asLongArray(String sep) {
        return asLongArray(sep, null);
    }

    public FileItem asFileItem() {
        if (!exists())
            return null;

        if (value instanceof FileItem && !((FileItem) value).isFormField())
            return (FileItem) value;

        return null;
    }
}
