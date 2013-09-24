package com.borqs.server.platform.sql;


public interface Table {
    int getShardCount();

    ShardResult getShard(int index);

    ShardResult shard(Object key);
}
