package com.borqs.server.platform.data;


import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonGenerator;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Addons {
    protected Map<String, Object> addons = null;

    public Addons() {
    }

    public boolean hasAddons() {
        return MapUtils.isNotEmpty(addons);
    }

    public boolean hasAddon(String col) {
        return addons != null && addons.containsKey(col);
    }

    public String[] getAddonColumns() {
        if (hasAddons()) {
            Set<String> cols = addons.keySet();
            return cols.toArray(new String[cols.size()]);
        } else {
            return new String[0];
        }
    }

    public void renameAddonColumn(String col, String newCol) {
        if (hasAddon(col)) {
            Object v = addons.get(col);
            addons.remove(col);
            addons.put(newCol, v);
        }
    }

    public void copyAddonColumn(String col, String newCol) {
        if (hasAddon(col)) {
            Object v = addons.get(col);
            addons.put(newCol, v);
        }
    }

    protected void ensureAddons() {
        if (addons == null)
            addons = new LinkedHashMap<String, Object>();
    }

    public void setAddon(String col, Object value) {
        Validate.notNull(col);
        ensureAddons();
        addons.put(col, value);
    }

    public void setAddons(String col, Map<String, Object> addons) {
        for (Map.Entry<String, Object> e : addons.entrySet())
            setAddon(e.getKey(), e.getValue());
    }

    public void removeAddon(String col) {
        if (hasAddons())
            addons.remove(col);
    }

    public Object getAddon(String col, Object def) {
        if (addons == null)
            return def;
        else
            return addons.containsKey(col) ? addons.get(col) : def;
    }

    public String getAddonAsString(String col, String def) {
        Object o = getAddon(col, null);
        return ObjectUtils.toString(o, def);
    }

    public Object checkAddon(String col) {
        Validate.isTrue(hasAddon(col));
        return addons.get(col);
    }

    public String checkAddonAsString(String col) {
        Object o = checkAddon(col);
        return ObjectUtils.toString(o);
    }

    public boolean getBooleanAddon(String col, boolean def) {
        return Values.toBoolean(getAddon(col, def));
    }

    public long getIntAddon(String col, long def) {
        return Values.toInt(getAddon(col, def));
    }

    public double getFloatAddon(String col, double def) {
        return Values.toFloat(getAddon(col, def));
    }

    public String getStringAddon(String col, String def) {
        return Values.toString(getAddon(col, def));
    }

    public void writeAddonsJson(JsonGenerator jg, String[] cols) throws IOException {
        if (hasAddons()) {
            for (Map.Entry<String, Object> e : addons.entrySet()) {
                String col = e.getKey();
                if (cols == null || ArrayUtils.contains(cols, col)) {
                    jg.writeFieldName(col);
                    Object v = e.getValue();
                    writeAddonValue(jg, v);
                }
            }
        }
    }

    public void writeAddonJson(JsonGenerator jg, String col) throws IOException {
        writeAddonJson(jg, col, null);
    }

    public void writeAddonJson(JsonGenerator jg, String col, AddonValueTransformer trans) throws IOException {
        if (hasAddon(col)) {
            jg.writeFieldName(col);
            Object v = addons.get(col);
            if (trans != null)
                v = trans.transform(v);
            writeAddonValue(jg, v);
        }
    }

    public void writeAddonJsonAs(JsonGenerator jg, String col, String asCol) throws IOException {
        writeAddonJsonAs(jg, col, asCol, null);
    }

    public void writeAddonJsonAs(JsonGenerator jg, String col, String asCol, AddonValueTransformer trans) throws IOException {
        if (hasAddon(col)) {
            jg.writeFieldName(asCol);
            Object v = addons.get(col);
            if (trans != null)
                v = trans.transform(v);
            writeAddonValue(jg, v);
        }
    }

    private static void writeAddonValue(JsonGenerator jg, Object v) throws IOException {
        if (!(v instanceof RawJsonValue)) {
            String json = JsonHelper.toJson(v, false);
            jg.writeRawValue(json);
        } else {
            jg.writeRawValue(v.toString());
        }
    }

    protected Map<String, Object> copyAddons() {
        return hasAddons() ? new LinkedHashMap<String, Object>(addons) : null;
    }

    public static class RawJsonValue {
        public final String json;

        public RawJsonValue(String json) {
            this.json = json;
        }

        @Override
        public String toString() {
            return json;
        }
    }

    public static RawJsonValue jsonAddonValue(String json) {
        return new RawJsonValue(json);
    }

    public boolean outputColumn(String[] expCols, String col) {
        if (expCols != null) {
            return !hasAddon(col) && ArrayUtils.contains(expCols, col);
        } else {
            return !hasAddon(col);
        }
    }

    public static interface AddonValueTransformer {
        Object transform(Object old);
    }
}
