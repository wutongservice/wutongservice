package com.borqs.server.base.sql;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class GroupedShardResults extends LinkedHashMap<ShardResult, List> {
    public GroupedShardResults() {
    }


    public GroupedShardResults addShardResult(ShardResult sr, Object key) {
        List keys = get(sr);
        if (keys == null) {
            keys = new ArrayList();
            put(sr, keys);
        }
        keys.add(key);
        return this;
    }

    public Set<ShardResult> getShardResults() {
        return keySet();
    }

    public List<ShardResult> getShardResultsList() {
        return new ArrayList<ShardResult>(getShardResults());
    }

    public ShardResult[] getShardResultArray() {
        Set<ShardResult> srs = getShardResults();
        return srs.toArray(new ShardResult[srs.size()]);
    }

    public List get(ShardResult sr) {
        return super.get(sr);
    }

    public String[] getShardResultDbs() {
        return ShardResult.getDbs(getShardResultsList());
    }
}
