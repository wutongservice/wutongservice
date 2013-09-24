package com.borqs.server.market.utils.mybatis.record;


import com.borqs.server.market.utils.record.Records;


public class RecordsWithTotal {
    private final Records records;
    private final int total;

    public RecordsWithTotal(Records records, int total) {
        this.records = records;
        this.total = total;
    }

    public Records getRecords() {
        return records;
    }

    public int getTotal() {
        return total;
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

    public int size() {
        return records.size();
    }
}
