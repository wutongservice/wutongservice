package com.borqs.server.base.migrate;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.util.Initializable;

public interface RecordInput extends Initializable {
    Record input();
}
