package com.borqs.server.platform.sql;


import org.apache.commons.lang.Validate;

public class SingleTable implements Table {
    private String db;
    private String table;

    public SingleTable() {
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    @Override
    public final int getShardCount() {
        return 1;
    }

    @Override
    public ShardResult getShard(int index) {
        Validate.isTrue(index >= 0 && index < getShardCount());
        return new ShardResult(db, table);
    }

    @Override
    public ShardResult shard(Object key) {
        return new ShardResult(db, table);
    }
}
