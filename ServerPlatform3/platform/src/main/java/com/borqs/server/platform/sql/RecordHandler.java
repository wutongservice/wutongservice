package com.borqs.server.platform.sql;

import com.borqs.server.platform.data.Record;

public interface RecordHandler {
    void handle(Record rec);
}
