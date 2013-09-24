package com.borqs.server.base.migrate;


import com.borqs.server.base.data.Record;

public interface RecordMigrateHandler {
    void handle(Record in, Record[] out) throws MigrateStopException;
}
