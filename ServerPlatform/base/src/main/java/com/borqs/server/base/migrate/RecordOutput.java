package com.borqs.server.base.migrate;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.util.Initializable;

public interface RecordOutput extends Initializable {
    void output(Record rec);
}
