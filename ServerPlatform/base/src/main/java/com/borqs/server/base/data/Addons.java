package com.borqs.server.base.data;


import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
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

    protected void ensureAddons() {
        if (addons == null)
            addons = new LinkedHashMap<String, Object>();
    }

    public void setAddon(String col, Object value) {
        Validate.notNull(col);
        ensureAddons();
        addons.put(col, Values.trimSimple(value));
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
        return addons.containsKey(col) ? addons.get(col) : def;
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

    protected void writeAddonsJson(JsonGenerator jg, String[] cols) throws IOException {
        if (hasAddons()) {
            for (Map.Entry<String, Object> e : addons.entrySet()) {
                String col = e.getKey();
                if (cols == null || ArrayUtils.contains(cols, col)) {
                    jg.writeFieldName(col);
                    jg.writeObject(e.getValue());
                }
            }
        }
    }

    protected Map<String, Object> copyAddons() {
        return hasAddons() ? new LinkedHashMap<String, Object>(addons) : null;
    }
    protected boolean outputColumn(String[] expCols, String col) {
        if (expCols != null) {
            return !hasAddon(col) && ArrayUtils.contains(expCols, col);
        } else {
            return !hasAddon(col);
        }
    }
}
