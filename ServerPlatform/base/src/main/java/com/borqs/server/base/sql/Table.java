package com.borqs.server.base.sql;


public interface Table {
    int getShardCount();

    ShardResult getShard(int index);

    ShardResult shard(Object key);
}
