package com.borqs.server.market.utils;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Params {
    private final LinkedHashMap<String, Object> params;
    public Params() {
        this(null);
    }

    public Params(Map<? extends String, ?> m) {
        params = m != null
                ? new LinkedHashMap<String, Object>(m)
                : new LinkedHashMap<String, Object>();
    }

    public LinkedHashMap<String, Object> getParams() {
        return params;
    }

    public boolean paramExists(String name) {
        return params.containsKey(name);
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

    public Param param(String name) {
        return paramExists(name) ? new Param(params.get(name)) : Param.NON_EXISTS;
    }

    public static Params of(Object... kvs) {
        Params params = new Params();
        CC.map(params.params, kvs);
        return params;
    }
}
