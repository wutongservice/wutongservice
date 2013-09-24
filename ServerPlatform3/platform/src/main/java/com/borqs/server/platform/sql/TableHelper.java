package com.borqs.server.platform.sql;

import com.borqs.server.platform.util.DateHelper;
import org.apache.commons.lang.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

public class TableHelper {

    public static boolean isNoShardTable(Table table) {
        return table.getShardCount() == 1;
    }

    public static boolean isShardedTable(Table table) {
        return table.getShardCount() > 1;
    }

    public static ShardResult shard(Table table, Object key) {
        return table.shard(key);
    }

    public static ShardResult[] getRandomShards(Table table, int count) {
        if (count <= 0)
            return new ShardResult[0];

        int shardCount = table.getShardCount();
        Random rand = new Random(DateHelper.nowNano());
        ShardResult[] srs = new ShardResult[count];
        for (int i = 0; i < count; i++) {
            srs[i] = table.getShard(rand.nextInt(shardCount));
        }
        return srs;
    }

    public static ShardResult[] getAllShards(Table table) {
        int shardCount = table.getShardCount();
        ShardResult[] srs = new ShardResult[shardCount];
        for (int i = 0; i < srs.length; i++)
            srs[i] = table.getShard(i);
        return srs;
    }

    public static ShardResult getRandomShard(Table table) {
        if (table.getShardCount() == 1) {
            return table.getShard(0);
        } else {
            Random rand = new Random(DateHelper.nowNano());
            return table.getShard(rand.nextInt(table.getShardCount()));
        }
    }

    public static GroupedShardResults shard(Table table, Collection<?> keys) {
        GroupedShardResults gsr = new GroupedShardResults();
        for (Object key : keys) {
            ShardResult sr = table.shard(key);
            gsr.addShardResult(sr, key);
        }
        return gsr;
    }

    public static GroupedShardResults shard(Table table, long[] keys) {
        return shard(table, Arrays.asList(ArrayUtils.toObject(keys)));
    }

    public static GroupedShardResults shard(Table table, String[] keys) {
        return shard(table, Arrays.asList(keys));
    }
}
