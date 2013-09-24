package com.borqs.server.market.utils;


import com.borqs.server.market.utils.record.Record;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import java.util.*;

public class Params {
    protected final LinkedHashMap<String, Object> params;

    public Params() {
        this((Map<String, Object>) null);
    }

    public Params(Map<String, ?> m) {
        params = m != null
                ? new LinkedHashMap<String, Object>(m)
                : new LinkedHashMap<String, Object>();
    }

    public Params(Params other) {
        params = other != null
                ? new LinkedHashMap<String, Object>(other.params)
                : new LinkedHashMap<String, Object>();
    }

    public Params putAll(Map<String, ?> m) {
        if (m != null) {
            params.putAll(m);
        }
        return this;
    }

    public LinkedHashMap<String, Object> getParams() {
        return params;
    }

    public boolean hasParam(String name) {
        return params.containsKey(name);
    }

    public boolean isMissingOrNull(String name) {
        return params.get(name) == null;
    }

    public boolean isNotMissingOrNull(String name) {
        return params.get(name) != null;
    }

    public boolean hasAllParams(String... names) {
        for (String name : names) {
            if (!hasParam(name))
                return false;
        }
        return true;
    }

    public boolean hasAnyParams(String... names) {
        for (String name : names) {
            if (hasParam(name))
                return true;
        }
        return false;
    }

    public Set<String> paramNamesSet() {
        return params.keySet();
    }

    public boolean isEmpty() {
        return params.isEmpty();
    }

    public int paramsCount() {
        return params.size();
    }

    public String[] paramsNames() {
        Set<String> set = paramNamesSet();
        return set.toArray(new String[set.size()]);
    }

    public Object[] paramsValues() {
        return params.values().toArray();
    }

    public Params put(String name, Object val) {
        params.put(name, val);
        return this;
    }

    public Object get(String name) {
        return params.get(name);
    }

    public Param param(String name) {
        return hasParam(name) ? new Param(params.get(name)) : Param.NON_EXISTS;
    }

    public static Params of(Object... kvs) {
        Params params = new Params();
        CC.map(params.params, kvs);
        return params;
    }

    public Params renameParam(String oldName, String newName) {
        if (newName != null && !StringUtils.equals(oldName, newName) && hasParam(oldName)) {
            Object val = params.remove(oldName);
            params.put(newName, val);
        }
        return this;
    }

    public Params renameParams(Map<String, String> oldNamesAndNewNames) {
        if (MapUtils.isNotEmpty(oldNamesAndNewNames)) {
            for (Map.Entry<String, String> e : oldNamesAndNewNames.entrySet())
                renameParam(e.getKey(), e.getValue());
        }
        return this;
    }

    public Params renameParams(String... oldNamesAndNewNames) {
        return renameParams(CC.strMap(oldNamesAndNewNames));
    }

    public Params removeParams(String... names) {
        for (String name : names) {
            params.remove(name);
        }
        return this;
    }

    public Params retainsParams(String... names) {
        HashSet<String> nameSet = new HashSet<String>(params.keySet());
        for (String name : nameSet) {
            if (!ArrayUtils.contains(names, name))
                params.remove(name);
        }
        return this;
    }


    public static void aggregateMultipleLocale(Map<String, Object> m, String name) {
        String prefix = name + "_";
        HashSet<String> removedNames = new HashSet<String>();
        ObjectNode mlNode = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<String, Object> entry : m.entrySet()) {
            String name0 = entry.getKey();
            String val0 = ObjectUtils.toString(entry.getValue());
            if (name0.equals(name)) {
                mlNode.put("default", val0);
            } else if (name0.startsWith(prefix)) {
                removedNames.add(name0);
                String l = StringUtils.removeStart(name0, prefix);
                if (l.isEmpty()) {
                    mlNode.put("default", val0);
                } else {
                    mlNode.put(l, val0);
                }
            }
        }
        if (!removedNames.isEmpty()) {
            m.put(name, mlNode);
            for (String name0 : removedNames)
                m.remove(name0);
        }
    }

    public Params aggregateMultipleLocale(String name) {
        aggregateMultipleLocale(params, name);
        return this;
    }

    public Params aggregateMultipleLocale(String... names) {
        for (String name : names) {
            aggregateMultipleLocale(name);
        }
        return this;
    }

    public static void disperseMultipleLocale(Map<String, Object> m, String name) {
        Object val = m.get(name);
        if (val instanceof JsonNode && ((JsonNode) val).isObject()) {
            m.remove(name);
            JsonNode mlNode = (JsonNode) val;
            Iterator<Map.Entry<String, JsonNode>> iter = mlNode.getFields();
            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                String locale = entry.getKey();
                String text = entry.getValue().asText();
                if ("default".equalsIgnoreCase(locale)) {
                    m.put(name, text);
                } else {
                    m.put(name + "_" + locale, text);
                }
            }
        }
    }

    public Params disperseMultipleLocale(String name) {
        disperseMultipleLocale(params, name);
        return this;
    }

    public Params disperseMultipleLocale(String... names) {
        for (String name : names) {
            disperseMultipleLocale(name);
        }
        return this;
    }

    public Paging getPaging(String pageField, String countField, int defaultCount) {
        int page = param(pageField).asInt(0);
        int count = param(countField).asInt(defaultCount);
        return new Paging(page, count);
    }

    public Paging getPaging(int defaultCount) {
        return getPaging("page", "count", defaultCount);
    }

    public Record asRecord(Record reuse) {
        Record rec = reuse != null ? reuse : new Record();
        rec.putAll(params);
        return rec;
    }

    public Record asRecord(Record reuse, String... names) {
        Record rec = reuse != null ? reuse : new Record();
        for (String name : names) {
            if (hasParam(name))
                rec.set(name, get(name));
        }
        return rec;
    }
}
