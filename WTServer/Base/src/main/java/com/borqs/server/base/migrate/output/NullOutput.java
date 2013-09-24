package com.borqs.server.base.migrate.output;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.migrate.RecordOutput;

public class NullOutput implements RecordOutput {
    @Override
    public void output(Record rec) {
    }

    @Override
    public void init() {
    }

    @Override
    public void destroy() {
    }
}
